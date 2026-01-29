package com.example.coccoc.ui.screen.normalarticle

import android.content.Context
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coccoc.R
import com.example.coccoc.domain.model.Article
import com.example.coccoc.utils.ContentSummarizer
import com.example.coccoc.utils.AudioDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    @ApplicationContext val context: Context,
) : ViewModel() {

    private val audioDownloadManager = AudioDownloadManager(context)
    private val contentSummarizer = ContentSummarizer(context)
    private val webContentExtractor = com.example.coccoc.utils.WebContentExtractor()

    private val _uiState = MutableStateFlow<ArticleDetailUiState>(ArticleDetailUiState.Loading)
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    private var webViewRef: WeakReference<WebView>? = null
    private var extractedWebContent: String? = null

    fun loadArticle(article: Article) {
        _uiState.value = ArticleDetailUiState.Success(article = article)
        Timber.d("Article loaded: ${article.title}")
    }

    fun setWebView(view: WebView) {
        webViewRef = WeakReference(view)
        Timber.d("WebView reference set")
    }

    fun addDetectedAudioUrl(url: String) {
        _uiState.update { currentState ->
            when (currentState) {
                is ArticleDetailUiState.Success -> {
                    val currentList = currentState.audioState.audioUrlsList.toMutableList()

                    if (currentList.none { it.url == url }) {
                        currentList.add(
                            AudioFileInfo(
                                url = url,
                                fileName = audioDownloadManager.getFileNameFromUrl(url)
                            )
                        )
                        Timber.d("Network audio detected: $url")

                        val isFirstDetection = !currentState.audioState.hasDetectedAudio

                        currentState.copy(
                            audioState = currentState.audioState.copy(
                                audioUrlsList = currentList,
                                hasDetectedAudio = true
                            ),
                            message = if (isFirstDetection)
                                context.getString(R.string.audio_detected, currentList.size)
                            else if (currentList.size > 1)
                                context.getString(R.string.audio_files_detected, currentList.size)
                            else null
                        )
                    } else {
                        currentState
                    }
                }
                else -> currentState
            }
        }
    }

    fun showDownloadDialog() {
        _uiState.update { currentState ->
            when (currentState) {
                is ArticleDetailUiState.Success -> {
                    if (currentState.audioState.audioUrlsList.isEmpty()) {
                        currentState.copy(
                            message = context.getString(R.string.no_audio_found)
                        )
                    } else {
                        currentState.copy(
                            audioState = currentState.audioState.copy(showDownloadDialog = true)
                        )
                    }
                }
                else -> currentState
            }
        }
    }

    fun hideDownloadDialog() {
        _uiState.update { currentState ->
            when (currentState) {
                is ArticleDetailUiState.Success -> {
                    currentState.copy(
                        audioState = currentState.audioState.copy(showDownloadDialog = false)
                    )
                }
                else -> currentState
            }
        }
    }

    fun downloadSelectedAudio(audioFileInfo: AudioFileInfo) {
        hideDownloadDialog()

        val currentState = _uiState.value
        if (currentState !is ArticleDetailUiState.Success) return
        if (currentState.audioState.isDownloading) return

        _uiState.value = currentState.copy(
            audioState = currentState.audioState.copy(isDownloading = true)
        )

        viewModelScope.launch {
            try {
                val result = audioDownloadManager.downloadAudio(audioFileInfo.url, audioFileInfo.fileName)
                if (result == -1L) {
                    _uiState.update { state ->
                        if (state is ArticleDetailUiState.Success) {
                            state.copy(
                                audioState = state.audioState.copy(isDownloading = false),
                                message = context.getString(R.string.download_failed)
                            )
                        } else state
                    }
                    Timber.e("Failed to download audio")
                } else {
                    _uiState.update { state ->
                        if (state is ArticleDetailUiState.Success) {
                            state.copy(
                                audioState = state.audioState.copy(isDownloading = false),
                                message = context.getString(R.string.download_started, audioFileInfo.fileName)
                            )
                        } else state
                    }
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    if (state is ArticleDetailUiState.Success) {
                        state.copy(
                            audioState = state.audioState.copy(isDownloading = false),
                            message = context.getString(R.string.error_summarizing, e.message ?: "Unknown")
                        )
                    } else state
                }
                Timber.e(e, "Error downloading audio")
            }
        }
    }

    fun summarizeContent() {
        val currentState = _uiState.value
        if (currentState !is ArticleDetailUiState.Success) return
        if (currentState.summarizationState.isSummarizing) return

        _uiState.value = currentState.copy(
            summarizationState = currentState.summarizationState.copy(isSummarizing = true)
        )

        viewModelScope.launch {
            try {
                // Get WebView reference
                val webView = webViewRef?.get()
                if (webView == null) {
                    _uiState.update { state ->
                        if (state is ArticleDetailUiState.Success) {
                            state.copy(
                                summarizationState = state.summarizationState.copy(isSummarizing = false),
                                message = context.getString(R.string.webview_not_ready)
                            )
                        } else state
                    }
                    return@launch
                }

                // Use efficient Readability-based extraction
                Timber.d(context.getString(R.string.extracting_content))
                val webContent = webContentExtractor.extractArticleContent(webView)

                if (webContent.isEmpty() || webContent.length < 50) {
                    _uiState.update { state ->
                        if (state is ArticleDetailUiState.Success) {
                            state.copy(
                                summarizationState = state.summarizationState.copy(isSummarizing = false),
                                message = context.getString(R.string.could_not_extract_content, webContent.length)
                            )
                        } else state
                    }
                    return@launch
                }

                Timber.d(context.getString(R.string.sending_to_ai, webContent.length))
                val summary = contentSummarizer.summarizeText(webContent)

                _uiState.update { state ->
                    if (state is ArticleDetailUiState.Success) {
                        state.copy(
                            summarizationState = state.summarizationState.copy(
                                summary = summary,
                                isSummarizing = false
                            ),
                            message = context.getString(R.string.summary_generated, webContent.length)
                        )
                    } else state
                }
                Timber.d("Content summarized successfully")
            } catch (e: Exception) {
                _uiState.update { state ->
                    if (state is ArticleDetailUiState.Success) {
                        state.copy(
                            summarizationState = state.summarizationState.copy(isSummarizing = false),
                            message = context.getString(R.string.error_summarizing, e.message ?: "Unknown")
                        )
                    } else state
                }
                Timber.e(e, "Error summarizing content")
            }
        }
    }

    fun clearMessage() {
        _uiState.update { currentState ->
            when (currentState) {
                is ArticleDetailUiState.Success -> currentState.copy(message = null)
                else -> currentState
            }
        }
    }

    fun clearSummary() {
        _uiState.update { currentState ->
            when (currentState) {
                is ArticleDetailUiState.Success -> currentState.copy(
                    summarizationState = currentState.summarizationState.copy(summary = "")
                )
                else -> currentState
            }
        }
    }

    override fun onCleared() {
        webViewRef?.clear()
        webViewRef = null
        extractedWebContent = null
        Timber.d("ArticleDetailViewModel cleared")
        super.onCleared()
    }
}
