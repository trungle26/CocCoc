package com.example.coccoc.ui.screen

import com.example.coccoc.domain.model.Article

data class ArticleListUiState(
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
