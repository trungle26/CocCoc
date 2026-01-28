package com.example.coccoc.service
import com.example.coccoc.domain.model.Article

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val audioUrl: String = "",
    val title: String = "",
    val article: Article? = null
)
