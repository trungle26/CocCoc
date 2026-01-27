package com.example.coccoc.ui.screen.normalarticle

import android.content.Context
import androidx.lifecycle.SavedStateHandle
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

    private val _uiState = MutableStateFlow(ArticleDetailUiState())
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    fun loadArticle(article: Article) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val result = getArticleDetailUseCase.execute(article)
                result.onSuccess { loadedArticle ->
                    _uiState.value = _uiState.value.copy(
                        article = loadedArticle,
                        isLoading = false,
                        error = null
                    )
                    Timber.d("Article loaded: ${loadedArticle.title}")
                }
                result.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Unknown error"
                    )
                    Timber.e(exception, "Failed to load article")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
                Timber.e(e, "Error loading article")
            }
        }
    }

    fun setExtractedAudioUrls(audioUrlsJson: String) {
        _uiState.value = _uiState.value.copy(extractedAudioUrls = audioUrlsJson)
        Timber.d("Audio URLs extracted: $audioUrlsJson")
    }

    fun extractAndDownloadAudio() {
        if (_uiState.value.isDownloadingAudio) return

        _uiState.value = _uiState.value.copy(isDownloadingAudio = true)
        viewModelScope.launch {
            try {
                val audioUrlsJson = _uiState.value.extractedAudioUrls
                if (audioUrlsJson == "[]" || audioUrlsJson.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isDownloadingAudio = false,
                        message = "No audio found in article"
                    )
                    return@launch
                }

                val audioUrls = try {
                    audioUrlsJson
                        .removeSurrounding("[", "]")
                        .split(",")
                        .map { it.trim().removeSurrounding("\"") }
                        .filter { it.isNotEmpty() }
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing audio URLs")
                    emptyList()
                }

                if (audioUrls.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isDownloadingAudio = false,
                        message = "No valid audio URLs found"
                    )
                    return@launch
                }

                val audioUrl = audioUrls.first()
                val fileName = audioDownloadManager.getFileNameFromUrl(audioUrl)

                val result = audioDownloadManager.downloadAudio(audioUrl, fileName)
                if(result == -1L){
                    _uiState.value = _uiState.value.copy(
                        isDownloadingAudio = false,
                        message = "Download failed}"
                    )
                    Timber.e("Failed to download audio")
                }else{
                    _uiState.value = _uiState.value.copy(
                        isDownloadingAudio = false,
                        message = "Audio downloading: $fileName"
                    )
                    Timber.d("Audio downloading: $fileName")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloadingAudio = false,
                    message = "Error: ${e.message}"
                )
                Timber.e(e, "Error downloading audio")
            }
        }
    }

    fun summarizeContent() {
        if (_uiState.value.isSummarizing) return

        _uiState.value = _uiState.value.copy(isSummarizing = true)
        viewModelScope.launch {
            try {
                val article = _uiState.value.article
                if (article == null || article.description.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isSummarizing = false,
                        message = "No content to summarize"
                    )
                    return@launch
                }

                val summary = contentSummarizer.summarizeHtml(article.description)
                _uiState.value = _uiState.value.copy(
                    summary = summary,
                    isSummarizing = false,
                    message = "Summary generated"
                )
                Timber.d("Content summarized: $summary")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSummarizing = false,
                    message = "Error: ${e.message}"
                )
                Timber.e(e, "Error summarizing content")
            }
        }
    }

    fun resetSummary() {
        _uiState.value = _uiState.value.copy(summary = "")
    }

    override fun onCleared() {
        Timber.d("ArticleDetailViewModel cleared")
        super.onCleared()
    }
}
