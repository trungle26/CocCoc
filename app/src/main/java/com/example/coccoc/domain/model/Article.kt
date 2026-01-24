package com.example.coccoc.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Article(
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String,
    val imageUrl: String?
)
