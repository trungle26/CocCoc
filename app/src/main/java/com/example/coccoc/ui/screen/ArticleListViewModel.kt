package com.example.coccoc.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coccoc.domain.usecase.GetNormalArticlesAsFlowUseCase
import com.example.coccoc.domain.usecase.GetPodcastArticlesAsFlowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val getNormalArticlesAsFlowUseCase: GetNormalArticlesAsFlowUseCase,
    private val getPodcastArticlesAsFlowUseCase: GetPodcastArticlesAsFlowUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ArticleListUiState())
    val uiState: StateFlow<ArticleListUiState> = _uiState.asStateFlow()

    init {
        loadArticles()
    }

    fun loadArticles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            combine(
                getNormalArticlesAsFlowUseCase(),
                getPodcastArticlesAsFlowUseCase()
            ) { normalResult, podcastResult ->
                val normalArticles = if (normalResult.isSuccess) {
                    normalResult.getOrNull() ?: emptyList()
                } else {
                    emptyList()
                }

                val podcastArticles = if (podcastResult.isSuccess) {
                    podcastResult.getOrNull() ?: emptyList()
                } else {
                    emptyList()
                }

                val allArticles = (podcastArticles + normalArticles).sortedByDescending { it.title }

                val error = when {
                    normalResult.isFailure -> normalResult.exceptionOrNull()?.message
                    podcastResult.isFailure -> podcastResult.exceptionOrNull()?.message
                    else -> null
                }

                Pair(allArticles, error)
            }.collect { (articles, error) ->
                _uiState.value = _uiState.value.copy(
                    articles = articles,
                    isLoading = false,
                    error = error
                )
            }
        }
    }
}
