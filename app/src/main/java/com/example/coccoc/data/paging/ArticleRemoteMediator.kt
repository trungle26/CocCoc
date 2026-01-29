package com.example.coccoc.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.example.coccoc.data.datasource.RemoteDataSource
import com.example.coccoc.data.local.AppDatabase
import com.example.coccoc.data.local.entity.ArticleEntity
import com.example.coccoc.data.mapper.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@OptIn(ExperimentalPagingApi::class)
class ArticleRemoteMediator(
    private val database: AppDatabase,
    private val remoteDataSource: RemoteDataSource
) : RemoteMediator<Int, ArticleEntity>() {

    private val articleDao = database.articleDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ArticleEntity>
    ): MediatorResult {
        return try {
            when (loadType) {
                LoadType.REFRESH -> {
                    Timber.d("RemoteMediator: REFRESH")

                    // Get seen article links BEFORE fetching new data
                    val seenLinks = articleDao.getSeenArticleLinks().toSet()
                    Timber.d("RemoteMediator: ${seenLinks.size} seen articles")

                    // Fetch new data from network
                    val normalArticles = withContext(Dispatchers.IO) {
                        remoteDataSource.fetchArticles()
                    }
                    val podcastArticles = withContext(Dispatchers.IO) {
                        remoteDataSource.fetchPodcastArticles()
                    }

                    val allArticles = (podcastArticles + normalArticles)

                    if (allArticles.isEmpty()) {
                        Timber.d("RemoteMediator: No articles fetched, keeping existing data")
                        // Don't clear if fetch returned empty - keep existing cached data
                        return MediatorResult.Success(endOfPaginationReached = true)
                    }

                    // Separate new and seen articles
                    val newArticles = allArticles.filter { it.link !in seenLinks }
                    val seenArticles = allArticles.filter { it.link in seenLinks }

                    Timber.d("RemoteMediator: ${newArticles.size} new, ${seenArticles.size} seen")

                    // Shuffle new articles for variety, seen articles keep order
                    val shuffledNewArticles = newArticles.shuffled()

                    // Create entities: new articles first (sortOrder 0-n), seen articles last (sortOrder n+1...)
                    val newEntities = shuffledNewArticles.mapIndexed { index, article ->
                        article.toEntity(isSeen = false, sortOrder = index)
                    }

                    val seenEntities = seenArticles.mapIndexed { index, article ->
                        article.toEntity(isSeen = true, sortOrder = shuffledNewArticles.size + index)
                    }

                    val allEntities = newEntities + seenEntities

                    // Clear and insert (only after we have new data)
                    articleDao.clearAll()
                    articleDao.insertAll(allEntities)

                    Timber.d("RemoteMediator: Inserted ${allEntities.size} articles (${newEntities.size} new first, ${seenEntities.size} seen last)")
                    MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.PREPEND -> {
                    Timber.d("RemoteMediator: PREPEND - end of pagination")
                    MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.APPEND -> {
                    Timber.d("RemoteMediator: APPEND - end of pagination")
                    MediatorResult.Success(endOfPaginationReached = true)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "RemoteMediator: Error loading articles")
            // On error, don't clear existing data - just report error
            MediatorResult.Error(e)
        }
    }

    override suspend fun initialize(): InitializeAction {
        val count = articleDao.getArticleCount()
        Timber.d("RemoteMediator: initialize, article count = $count")
        return if (count > 0) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }
}
