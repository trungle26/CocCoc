package com.example.coccoc.domain.repository

import com.example.coccoc.domain.model.Article
import kotlinx.coroutines.flow.Flow

interface IArticleRepository {
    fun getArticles(): Flow<Result<List<Article>>>
}
