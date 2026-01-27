package com.example.coccoc.ui.screen.normalarticle

import com.example.coccoc.domain.model.Article

data class ArticleDetailUiState(
    val article: Article? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val summary: String = "",
    val isSummarizing: Boolean = false,
    val extractedAudioUrls: String = "[]",
    val isDownloadingAudio: Boolean = false,
    val message: String? = null
)
