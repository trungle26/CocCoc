package com.example.coccoc.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.coccoc.R
import com.example.coccoc.domain.model.Article
import com.example.coccoc.ui.component.ArticleCard
import com.example.coccoc.ui.component.CompactArticleCard
import com.example.coccoc.ui.component.ErrorSection
import com.example.coccoc.ui.component.FeaturedArticleCard
import com.example.coccoc.ui.component.LoadingIndicator
import com.example.coccoc.utils.onVisible

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    onArticleClick: (Article) -> Unit,
    viewModel: ArticleListViewModel = hiltViewModel()
) {
    val articlesPaging = viewModel.articlesPagingFlow.collectAsLazyPagingItems()
    val refreshError by viewModel.refreshError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(refreshError) {
        refreshError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearRefreshError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.daily_news),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            articlesPaging.refresh()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = androidx.compose.ui.res.stringResource(R.string.refresh)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = articlesPaging.loadState.refresh is LoadState.Loading,
            onRefresh = {
                articlesPaging.refresh()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (articlesPaging.loadState.refresh) {
                is LoadState.Loading if articlesPaging.itemCount == 0 -> {
                    LoadingIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                is LoadState.Error if articlesPaging.itemCount == 0 -> {
                    val error = (articlesPaging.loadState.refresh as LoadState.Error).error
                    ErrorSection(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        error = error.message.toString()
                    ) {
                        articlesPaging.refresh()
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        val itemCount = articlesPaging.itemCount

                        // First 10 articles: Randomized newspaper-style layout
                        // Pattern: Featured -> 2 Compact -> Featured -> Regular -> 2 Compact -> Regular -> Featured -> Regular
                        val featuredLayout = listOf(0, 3, 8) // Featured articles positions
                        val compactPairLayout = listOf(1, 5) // Start positions for compact pairs

                        var index = 0
                        while (index < minOf(itemCount, 10)) {
                            when (val currentIndex = index) {
                                in featuredLayout -> {
                                    item(
                                        key = articlesPaging[currentIndex]?.link
                                            ?: "featured_$currentIndex"
                                    ) {
                                        val article = articlesPaging[currentIndex]
                                        if (article != null) {
                                            FeaturedArticleCard(
                                                article = article,
                                                onClick = { onArticleClick(article) },
                                                modifier = Modifier.onVisible(article.link) {
                                                    viewModel.markArticleAsSeen(article.link)
                                                }
                                            )
                                        }
                                    }
                                    index++
                                }

                                // Compact side-by-side pairs
                                in compactPairLayout if currentIndex + 1 < itemCount -> {
                                    item(key = "compact_row_$currentIndex") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val article1 = articlesPaging[currentIndex]
                                            val article2 = articlesPaging[currentIndex + 1]

                                            if (article1 != null) {
                                                CompactArticleCard(
                                                    article = article1,
                                                    onClick = { onArticleClick(article1) },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .onVisible(article1.link) {
                                                            viewModel.markArticleAsSeen(article1.link)
                                                        }
                                                )
                                            }
                                            if (article2 != null) {
                                                CompactArticleCard(
                                                    article = article2,
                                                    onClick = { onArticleClick(article2) },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .onVisible(article2.link) {
                                                            viewModel.markArticleAsSeen(article2.link)
                                                        }
                                                )
                                            }
                                        }
                                    }
                                    index += 2
                                }

                                // Regular articles for other positions
                                else -> {
                                    item(
                                        key = articlesPaging[currentIndex]?.link
                                            ?: "regular_$currentIndex"
                                    ) {
                                        val article = articlesPaging[currentIndex]
                                        if (article != null) {
                                            ArticleCard(
                                                article = article,
                                                onClick = { onArticleClick(article) },
                                                modifier = Modifier.onVisible(article.link) {
                                                    viewModel.markArticleAsSeen(article.link)
                                                }
                                            )
                                        }
                                    }
                                    index++
                                }
                            }
                        }

                        // Remaining articles: Simple regular cards (less care, more efficient)
                        if (itemCount > 10) {
                            items(
                                count = itemCount - 10,
                                key = { i -> articlesPaging[i + 10]?.link ?: "rest_${i + 10}" }
                            ) { i ->
                                val article = articlesPaging[i + 10]
                                if (article != null) {
                                    ArticleCard(
                                        article = article,
                                        onClick = { onArticleClick(article) },
                                        modifier = Modifier.onVisible(article.link) {
                                            viewModel.markArticleAsSeen(article.link)
                                        }
                                    )
                                }
                            }
                        }

                        if (articlesPaging.loadState.append is LoadState.Loading) {
                            item {
                                LoadingIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
