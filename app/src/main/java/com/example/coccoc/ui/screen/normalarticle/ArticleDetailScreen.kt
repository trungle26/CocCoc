package com.example.coccoc.ui.screen.normalarticle

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.coccoc.domain.model.Article
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ArticleDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: ArticleDetailViewModel = hiltViewModel(),
    article: Article,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val webViewReference = remember { mutableListOf<WebView>() }

    val notificationPermissionState = if (android.os.Build.VERSION.SDK_INT >= 33) {
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    } else null

    LaunchedEffect(article.link) {
        viewModel.loadArticle(article)
    }

    val currentMessage = if (uiState is ArticleDetailUiState.Success) {
        (uiState as ArticleDetailUiState.Success).message
    } else null

    LaunchedEffect(currentMessage) {
        currentMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewReference.forEach { webView ->
                webView.stopLoading()
                webView.clearHistory()
                webView.clearCache(true)
                webView.destroy()
            }
            webViewReference.clear()
        }
    }

    // Scaffold wraps all states for consistent back button
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = article.title,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState is ArticleDetailUiState.Success) {
                        val state = uiState as ArticleDetailUiState.Success
                        IconButton(
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= 33 &&
                                    notificationPermissionState?.status?.isGranted == false) {
                                    notificationPermissionState.launchPermissionRequest()
                                }
                                viewModel.showDownloadDialog()
                            },
                            enabled = state.audioState.hasDetectedAudio && !state.audioState.isDownloading
                        ) {
                            if (state.audioState.isDownloading) {
                                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Download Audio",
                                    tint = if (state.audioState.hasDetectedAudio)
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.summarizeContent() },
                            enabled = !state.summarizationState.isSummarizing
                        ) {
                            if (state.summarizationState.isSummarizing) {
                                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Create,
                                    contentDescription = "Summarize"
                                )
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when (val state = uiState) {
            is ArticleDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ArticleDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = state.errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { viewModel.loadArticle(article) }) {
                            Text("Retry")
                        }
                    }
                }
            }

            is ArticleDetailUiState.Success -> {
                ArticleDetailContent(
                    modifier = modifier,
                    state = state,
                    article = article,
                    viewModel = viewModel,
                    webViewReference = webViewReference,
                    innerPadding = innerPadding
                )
            }
        }
    }
}

@Composable
private fun ArticleDetailContent(
    modifier: Modifier,
    state: ArticleDetailUiState.Success,
    article: Article,
    viewModel: ArticleDetailViewModel,
    webViewReference: MutableList<WebView>,
    innerPadding: androidx.compose.foundation.layout.PaddingValues
) {
    if (state.audioState.showDownloadDialog) {
        var selectedIndex by remember { mutableIntStateOf(0) }

        AlertDialog(
            onDismissRequest = { viewModel.hideDownloadDialog() },
            title = {
                Text(
                    text = if (state.audioState.audioUrlsList.size > 1)
                        "Select Audio to Download"
                    else
                        "Download Audio"
                )
            },
            text = {
                Column {
                    if (state.audioState.audioUrlsList.size > 1) {
                        Text(
                            text = "${state.audioState.audioUrlsList.size} audio files found. Select one to download:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    LazyColumn {
                        items(state.audioState.audioUrlsList.size) { index ->
                            val audioInfo = state.audioState.audioUrlsList[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedIndex = index }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (state.audioState.audioUrlsList.size > 1) {
                                        RadioButton(
                                            selected = selectedIndex == index,
                                            onClick = { selectedIndex = index }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = audioInfo.fileName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Will be saved to Downloads folder",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (state.audioState.audioUrlsList.isNotEmpty()) {
                            viewModel.downloadSelectedAudio(state.audioState.audioUrlsList[selectedIndex])
                        }
                    }
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDownloadDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            request?.url?.toString()?.let { urlString ->
                                if (isAudioUrl(urlString)) {
                                    viewModel.addDetectedAudioUrl(urlString)
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    loadUrl(article.link)
                    webViewReference.add(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun isAudioUrl(url: String): Boolean {
    val lowerUrl = url.lowercase(Locale.ROOT)
    return lowerUrl.endsWith(".mp3") ||
            lowerUrl.endsWith(".m4a") ||
            lowerUrl.endsWith(".wav") ||
            lowerUrl.endsWith(".aac") ||
            lowerUrl.endsWith(".ogg")
}
