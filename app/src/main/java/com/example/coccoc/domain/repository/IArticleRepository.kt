package com.example.coccoc.domain.repository

import androidx.paging.PagingData
import com.example.coccoc.domain.model.Article
import kotlinx.coroutines.flow.Flow

interface IArticleRepository {
    fun getArticlesPaged(): Flow<PagingData<Article>>
    suspend fun refreshArticles()
    suspend fun markArticleAsSeen(link: String)
}
