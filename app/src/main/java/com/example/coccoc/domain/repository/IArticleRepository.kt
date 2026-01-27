package com.example.coccoc.domain.repository

import com.example.coccoc.domain.model.Article
import kotlinx.coroutines.flow.Flow

interface IArticleRepository {
    fun getNormalArticles(): Flow<Result<List<Article>>>
    fun getPodcastArticles(): Flow<Result<List<Article>>>
}
