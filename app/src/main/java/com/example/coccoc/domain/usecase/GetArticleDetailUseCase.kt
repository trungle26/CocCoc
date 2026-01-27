package com.example.coccoc.domain.usecase

import com.example.coccoc.domain.model.Article
import javax.inject.Inject

class GetArticleDetailUseCase @Inject constructor() {
    suspend fun execute(article: Article): Result<Article> {
        return try {
            // Article detail loading logic
            // In the future, could fetch full content from web
            Result.success(article)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
