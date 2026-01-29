package com.example.coccoc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey
    val id: String, // Using link as unique ID
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String,
    val imageUrl: String?,
    val audioUrl: String?,
    val duration: String?,
    val type: String?, // "podcast" or null for normal
    val createdAt: Long = System.currentTimeMillis(),
    val isSeen: Boolean = false, // Track if article was visible on screen
    val sortOrder: Int = 0 // For custom sorting (new articles first, seen articles last)
)
