package com.example.coccoc.ui.screen.podcastarticle

import com.example.coccoc.domain.model.Article

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L
)

sealed class PodcastDetailUiState {
    data object Loading : PodcastDetailUiState()

    data class Success(
        val podcast: Article,
        val playbackState: PlaybackState = PlaybackState(),
        val isDownloading: Boolean = false,
        val message: String? = null
    ) : PodcastDetailUiState()

    data class Error(
        val errorMessage: String
    ) : PodcastDetailUiState()
}
