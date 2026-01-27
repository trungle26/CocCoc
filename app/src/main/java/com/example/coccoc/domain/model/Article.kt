package com.example.coccoc.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Article(
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String,
    val imageUrl: String?,
    val audioUrl: String? = null,
    val duration: String? = null,
    val type: String? = null // "podcast" or null for normal
)
