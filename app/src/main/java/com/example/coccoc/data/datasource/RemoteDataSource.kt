package com.example.coccoc.data.datasource

import com.example.coccoc.domain.model.Article
import com.example.coccoc.data.network.ApiService
import javax.inject.Inject

class RemoteDataSource @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getArticles(): List<Article> {
        return try {
            apiService.getArticles().articles
        } catch (e: Exception) {
            // Return mock data in case of network error
            getMockArticles()
        }
    }

    private fun getMockArticles(): List<Article> {
        return listOf(
            Article(
                id = "1",
                title = "Breaking News: Tech Industry Updates",
                description = "Latest updates from the tech industry with insights on AI and machine learning",
                url = "https://example.com/article1",
                image = "https://via.placeholder.com/150",
                source = "Tech News Daily",
                publishedAt = "2024-01-22T10:00:00Z",
                type = "normal"
            ),
            Article(
                id = "2",
                title = "Podcast: Future of Technology",
                description = "Expert discussion on the future of technology and innovation trends",
                url = "https://example.com/podcast1",
                image = "https://via.placeholder.com/150",
                source = "Tech Podcast Network",
                publishedAt = "2024-01-21T15:30:00Z",
                type = "podcast",
                audioUrl = "https://example.com/audio/podcast1.mp3"
            ),
            Article(
                id = "3",
                title = "Business: Market Analysis Q1 2024",
                description = "Comprehensive market analysis for the first quarter of 2024",
                url = "https://example.com/article3",
                image = "https://via.placeholder.com/150",
                source = "Business Weekly",
                publishedAt = "2024-01-20T12:00:00Z",
                type = "normal"
            ),
            Article(
                id = "4",
                title = "Science Podcast: Space Exploration",
                description = "Deep dive into recent space exploration missions and discoveries",
                url = "https://example.com/podcast2",
                image = "https://via.placeholder.com/150",
                source = "Science Today",
                publishedAt = "2024-01-19T14:00:00Z",
                type = "podcast",
                audioUrl = "https://example.com/audio/podcast2.m4a"
            )
        )
    }
}
