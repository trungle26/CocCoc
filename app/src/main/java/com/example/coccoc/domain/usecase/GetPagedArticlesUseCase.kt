package com.example.coccoc.domain.usecase

import androidx.paging.PagingData
import com.example.coccoc.domain.model.Article
import com.example.coccoc.domain.repository.IArticleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPagedArticlesUseCase @Inject constructor(
    private val repository: IArticleRepository
) {
    operator fun invoke(): Flow<PagingData<Article>> = repository.getArticlesPaged()
}
