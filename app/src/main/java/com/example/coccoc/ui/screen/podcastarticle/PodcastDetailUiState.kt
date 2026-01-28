package com.example.coccoc.ui.screen.podcastarticle

import com.example.coccoc.domain.model.Article

data class PodcastDetailUiState(
    val podcast: Article? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isDownloading: Boolean = false,
    val message: String? = null
)
