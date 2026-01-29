package com.example.coccoc.ui.screen.normalarticle

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coccoc.domain.model.Article
import com.example.coccoc.domain.usecase.GetArticleDetailUseCase
import com.example.coccoc.utils.ContentSummarizer
import com.example.coccoc.utils.audio.AudioDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val getArticleDetailUseCase: GetArticleDetailUseCase,
) : ViewModel() {

    private val audioDownloadManager = AudioDownloadManager(context)
    private val contentSummarizer = ContentSummarizer()

    private val _uiState = MutableStateFlow<ArticleDetailUiState>(ArticleDetailUiState.Loading)
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    fun loadArticle(article: Article) {
        _uiState.value = ArticleDetailUiState.Loading
        viewModelScope.launch {
            try {
                val result = getArticleDetailUseCase.execute(article)
                result.onSuccess { loadedArticle ->
                    _uiState.value = ArticleDetailUiState.Success(article = loadedArticle)
                    Timber.d("Article loaded: ${loadedArticle.title}")
                }
                result.onFailure { exception ->
                    _uiState.value = ArticleDetailUiState.Error(
                        errorMessage = exception.message ?: "Unknown error"
                    )
                    Timber.e(exception, "Failed to load article")
                }
            } catch (e: Exception) {
                _uiState.value = ArticleDetailUiState.Error(
                    errorMessage = e.message ?: "Unknown error"
                )
                Timber.e(e, "Error loading article")
            }
        }
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
                            message = if (isFirstDetection) "Audio detected! (${currentList.size} file(s))"
                                     else if (currentList.size > 1) "${currentList.size} audio files detected"
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
                        currentState.copy(message = "No audio found in article")
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
                                message = "Download failed"
                            )
                        } else state
                    }
                    Timber.e("Failed to download audio")
                } else {
                    _uiState.update { state ->
                        if (state is ArticleDetailUiState.Success) {
                            state.copy(
                                audioState = state.audioState.copy(isDownloading = false),
                                message = "Download started: ${audioFileInfo.fileName}"
                            )
                        } else state
                    }
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    if (state is ArticleDetailUiState.Success) {
                        state.copy(
                            audioState = state.audioState.copy(isDownloading = false),
                            message = "Error: ${e.message}"
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
                val article = currentState.article
                if (article.description.isEmpty()) {
                    _uiState.update { state ->
                        if (state is ArticleDetailUiState.Success) {
                            state.copy(
                                summarizationState = state.summarizationState.copy(isSummarizing = false),
                                message = "No content to summarize"
                            )
                        } else state
                    }
                    return@launch
                }

                val summary = contentSummarizer.summarizeHtml(article.description)
                _uiState.update { state ->
                    if (state is ArticleDetailUiState.Success) {
                        state.copy(
                            summarizationState = state.summarizationState.copy(
                                summary = summary,
                                isSummarizing = false
                            ),
                            message = "Summary generated"
                        )
                    } else state
                }
                Timber.d("Content summarized: $summary")
            } catch (e: Exception) {
                _uiState.update { state ->
                    if (state is ArticleDetailUiState.Success) {
                        state.copy(
                            summarizationState = state.summarizationState.copy(isSummarizing = false),
                            message = "Error: ${e.message}"
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
        Timber.d("ArticleDetailViewModel cleared")
        super.onCleared()
    }
}
