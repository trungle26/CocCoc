package com.example.coccoc.domain.usecase

import com.example.coccoc.domain.model.Article
import com.example.coccoc.domain.repository.IArticleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNormalArticlesAsFlowUseCase @Inject constructor(
    private val repository: IArticleRepository
) {
    operator fun invoke(): Flow<Result<List<Article>>> = repository.getNormalArticles()
}
