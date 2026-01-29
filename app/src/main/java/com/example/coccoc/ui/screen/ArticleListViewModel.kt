package com.example.coccoc.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.coccoc.domain.model.Article
import com.example.coccoc.domain.repository.IArticleRepository
import com.example.coccoc.domain.usecase.GetPagedArticlesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleListViewModel @Inject constructor(
    getPagedArticlesUseCase: GetPagedArticlesUseCase,
    private val repository: IArticleRepository
) : ViewModel() {

    val articlesPagingFlow: Flow<PagingData<Article>> = getPagedArticlesUseCase()
        .cachedIn(viewModelScope)

    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    private val seenInSession = mutableSetOf<String>()

    fun markArticleAsSeen(link: String) {
        if (link in seenInSession) return
        seenInSession.add(link)

        viewModelScope.launch {
            repository.markArticleAsSeen(link)
        }
    }

    fun clearRefreshError() {
        _refreshError.value = null
    }
}
