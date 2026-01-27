package com.example.coccoc.domain.usecase

import com.example.coccoc.domain.model.Article
import javax.inject.Inject

class GetPodcastDetailUseCase @Inject constructor() {
    suspend fun execute(article: Article): Result<Article> {
        return try {
            // Podcast detail loading logic
            // Validate audio URL exists
            if (article.audioUrl.isNullOrEmpty()) {
                return Result.failure(Exception("No audio URL provided"))
            }
            Result.success(article)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
