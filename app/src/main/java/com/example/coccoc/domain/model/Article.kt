package com.example.coccoc.domain.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Article(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("description")
    val description: String,
    @SerialName("url")
    val url: String,
    @SerialName("image")
    val image: String = "",
    @SerialName("source")
    val source: String = "",
    @SerialName("publishedAt")
    val publishedAt: String = "",
    @SerialName("type")
    val type: String = "normal", // "normal" or "podcast"
    @SerialName("audioUrl")
    val audioUrl: String = "",
    @SerialName("summary")
    val summary: String = ""
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ArticleResponse(
    @SerialName("articles")
    val articles: List<Article>,
    @SerialName("status")
    val status: String = "ok"
)

// For displaying articles in list
enum class ArticleType {
    NORMAL, PODCAST
}
