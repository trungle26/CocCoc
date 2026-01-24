package com.example.coccoc.data.network

import com.example.coccoc.domain.model.ArticleResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("articles")
    suspend fun getArticles(
        @Query("country") country: String = "us",
        @Query("sortBy") sortBy: String = "publishedAt"
    ): ArticleResponse

    companion object {
        const val BASE_URL = "https://newsapi.org/v2/"
    }
}
