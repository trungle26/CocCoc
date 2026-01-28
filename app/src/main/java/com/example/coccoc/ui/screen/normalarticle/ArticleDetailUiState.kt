package com.example.coccoc.ui.screen.normalarticle

import com.example.coccoc.domain.model.Article

data class AudioFileInfo(
    val url: String,
    val fileName: String,
)

data class AudioDetectionState(
    val audioUrlsList: List<AudioFileInfo> = emptyList(),
    val hasDetectedAudio: Boolean = false,
    val showDownloadDialog: Boolean = false,
    val isDownloading: Boolean = false
)

data class SummarizationState(
    val summary: String = "",
    val isSummarizing: Boolean = false
)

sealed class ArticleDetailUiState {
    data object Loading : ArticleDetailUiState()

    data class Success(
        val article: Article,
        val audioState: AudioDetectionState = AudioDetectionState(),
        val summarizationState: SummarizationState = SummarizationState(),
        val message: String? = null
    ) : ArticleDetailUiState()

    data class Error(
        val errorMessage: String
    ) : ArticleDetailUiState()
}
