package com.example.coccoc.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ArticleDetailUiState(
    val articleUrl: String = "",
    val summary: String = "",
    val isSummarizing: Boolean = false,
    val audioUrl: String = "",
    val isDownloadingAudio: Boolean = false,
    val downloadProgress: Int = 0
)

class ArticleDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ArticleDetailUiState())
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    fun setArticleUrl(url: String) {
        _uiState.value = _uiState.value.copy(articleUrl = url)
    }

    fun setAudioUrl(audioUrl: String) {
        _uiState.value = _uiState.value.copy(audioUrl = audioUrl)
    }

    fun summarizeContent(content: String) {
        _uiState.value = _uiState.value.copy(isSummarizing = true)
        // TODO: Implement actual summarization logic
        // For now, create a simple summary
        val summary = generateSimpleSummary(content)
        _uiState.value = _uiState.value.copy(
            summary = summary,
            isSummarizing = false
        )
    }

    fun resetSummary() {
        _uiState.value = _uiState.value.copy(summary = "")
    }

    private fun generateSimpleSummary(content: String): String {
        // Simple sentence-based summary (takes first 3 sentences)
        val sentences = content.split(Regex("[.!?]+"))
        return sentences.take(3)
            .joinToString(". ") { it.trim() }
            .take(200) + "..."
    }

    fun updateDownloadProgress(progress: Int) {
        _uiState.value = _uiState.value.copy(downloadProgress = progress)
    }

    fun setDownloadingAudio(isDownloading: Boolean) {
        _uiState.value = _uiState.value.copy(isDownloadingAudio = isDownloading)
    }
}
