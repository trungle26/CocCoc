package com.example.coccoc.ui.screen.normalarticle

import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.coccoc.domain.model.Article

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(article.link) {
        viewModel.loadArticle(article)
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
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
                    IconButton(
                        onClick = { viewModel.extractAndDownloadAudio() },
                        enabled = !uiState.isDownloadingAudio
                    ) {
                        if (uiState.isDownloadingAudio) {
                            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Download Audio"
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.summarizeContent() },
                        enabled = !uiState.isSummarizing
                    ) {
                        if (uiState.isSummarizing) {
                            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Create,
                                contentDescription = "Summarize"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                // Inject JavaScript to extract audio URLs
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        var audios = [];
                                        // Get all audio elements
                                        var audioElements = document.querySelectorAll('audio');
                                        audioElements.forEach(function(audio) {
                                            var src = audio.src || audio.querySelector('source')?.src;
                                            if (src) audios.push(src);
                                        });
                                        // Get all audio links
                                        var links = document.querySelectorAll('a[href*=".mp3"], a[href*=".m4a"], a[href*=".wav"]');
                                        links.forEach(function(link) {
                                            if (link.href) audios.push(link.href);
                                        });
                                        return JSON.stringify(audios);
                                    })();
                                    """.trimIndent()
                                ) { result ->
                                    try {
                                        val cleanResult = result?.removeSurrounding("\"") ?: "[]"
                                        viewModel.setExtractedAudioUrls(cleanResult)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
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
}
