package com.example.coccoc.ui.screen.normalarticle

import com.example.coccoc.domain.model.Article

data class ArticleDetailUiState(
    val article: Article? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
