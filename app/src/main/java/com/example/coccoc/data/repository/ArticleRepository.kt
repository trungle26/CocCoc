package com.example.coccoc.data.repository

import com.example.coccoc.data.datasource.RemoteDataSource
import com.example.coccoc.domain.model.Article
import com.example.coccoc.domain.repository.IArticleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ArticleRepository @Inject constructor(
    private val remoteDataSource: RemoteDataSource
) : IArticleRepository {
    override fun getArticles(): Flow<Result<List<Article>>> = flow {
        try {
            emit(Result.success(remoteDataSource.getArticles()))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
