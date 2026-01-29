package com.example.coccoc.data.mapper

import com.example.coccoc.data.local.entity.ArticleEntity
import com.example.coccoc.domain.model.Article

fun ArticleEntity.toDomain(): Article {
    return Article(
        title = title,
        link = link,
        description = description,
        pubDate = pubDate,
        imageUrl = imageUrl,
        audioUrl = audioUrl,
        duration = duration,
        type = type
    )
}

fun Article.toEntity(isSeen: Boolean = false, sortOrder: Int = 0): ArticleEntity {
    return ArticleEntity(
        id = link,
        title = title,
        link = link,
        description = description,
        pubDate = pubDate,
        imageUrl = imageUrl,
        audioUrl = audioUrl,
        duration = duration,
        type = type,
        isSeen = isSeen,
        sortOrder = sortOrder
    )
}
