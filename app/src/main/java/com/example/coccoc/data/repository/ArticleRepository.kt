package com.example.coccoc.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.coccoc.data.datasource.RemoteDataSource
import com.example.coccoc.data.local.AppDatabase
import com.example.coccoc.data.mapper.toDomain
import com.example.coccoc.data.mapper.toEntity
import com.example.coccoc.data.paging.ArticleRemoteMediator
import com.example.coccoc.di.IoDispatcher
import com.example.coccoc.domain.model.Article
import com.example.coccoc.domain.repository.IArticleRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

class ArticleRepository @Inject constructor(
    private val remoteDataSource: RemoteDataSource,
    private val database: AppDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IArticleRepository {

    private val articleDao = database.articleDao()

    @OptIn(ExperimentalPagingApi::class)
    override fun getArticlesPaged(): Flow<PagingData<Article>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            remoteMediator = ArticleRemoteMediator(database, remoteDataSource),
            pagingSourceFactory = { articleDao.getArticlesPagingSource() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override suspend fun refreshArticles() {
        withContext(ioDispatcher) {
            try {
                val normalArticles = remoteDataSource.fetchArticles()
                val podcastArticles = remoteDataSource.fetchPodcastArticles()
                val allEntities = (podcastArticles + normalArticles).map { it.toEntity() }

                articleDao.clearAll()
                articleDao.insertAll(allEntities)
                Timber.d("refreshArticles: Inserted ${allEntities.size} articles")
            } catch (e: Exception) {
                Timber.e(e, "refreshArticles failed")
                throw e
            }
        }
    }

    override suspend fun markArticleAsSeen(link: String) {
        withContext(ioDispatcher) {
            articleDao.markAsSeen(link)
        }
    }
}
