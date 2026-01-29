package com.example.coccoc.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.coccoc.data.local.entity.ArticleEntity

@Dao
interface ArticleDao {
    // Sort only by sortOrder - the RemoteMediator assigns sortOrder during refresh
    // based on seen status (unseen first, seen last)
    @Query("SELECT * FROM articles ORDER BY sortOrder ASC")
    fun getArticlesPagingSource(): PagingSource<Int, ArticleEntity>

    @Query("SELECT * FROM articles WHERE type IS NULL ORDER BY sortOrder ASC")
    fun getNormalArticlesPagingSource(): PagingSource<Int, ArticleEntity>

    @Query("SELECT * FROM articles WHERE type = 'podcast' ORDER BY sortOrder ASC")
    fun getPodcastArticlesPagingSource(): PagingSource<Int, ArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles")
    suspend fun clearAll()

    @Query("DELETE FROM articles WHERE type IS NULL")
    suspend fun clearNormalArticles()

    @Query("DELETE FROM articles WHERE type = 'podcast'")
    suspend fun clearPodcastArticles()

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun getArticleCount(): Int

    @Query("SELECT * FROM articles WHERE link = :link LIMIT 1")
    suspend fun getArticleByLink(link: String): ArticleEntity?

    // Mark article as seen
    @Query("UPDATE articles SET isSeen = 1 WHERE link = :link")
    suspend fun markAsSeen(link: String)

    // Get all seen article links
    @Query("SELECT link FROM articles WHERE isSeen = 1")
    suspend fun getSeenArticleLinks(): List<String>

    // Get all articles (not paging, for refresh logic)
    @Query("SELECT * FROM articles")
    suspend fun getAllArticles(): List<ArticleEntity>
}
