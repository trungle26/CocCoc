package com.example.coccoc.data.repository

import com.example.coccoc.data.datasource.RemoteDataSource
import com.example.coccoc.di.IoDispatcher
import com.example.coccoc.domain.model.Article
import com.example.coccoc.domain.repository.IArticleRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber

class ArticleRepository @Inject constructor(
    private val remoteDataSource: RemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IArticleRepository {
    override fun getArticles(): Flow<Result<List<Article>>> = flow {
        try {
            Timber.d("getArticles")
            val articles = withContext(ioDispatcher) {
                remoteDataSource.fetchArticles()
            }
            emit(Result.success(articles))
        } catch (e: Exception) {
            Timber.d("getArticles exception: $e")
            emit(Result.failure(e))
        }
    }
}
