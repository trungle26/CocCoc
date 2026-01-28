package com.example.coccoc.ui.screen.normalarticle

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    val notificationPermissionState = if (android.os.Build.VERSION.SDK_INT >= 33) {
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    } else null

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
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= 33 &&
                                notificationPermissionState?.status?.isGranted == false) {
                                notificationPermissionState.launchPermissionRequest()
                            }

                            viewModel.extractAndDownloadAudio { path ->
                                Toast.makeText(context, path, Toast.LENGTH_LONG).show()
                            }
                        },
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
}

private fun isAudioUrl(url: String): Boolean {
    val lowerUrl = url.lowercase(Locale.ROOT)
    return lowerUrl.endsWith(".mp3") ||
            lowerUrl.endsWith(".m4a") ||
            lowerUrl.endsWith(".wav") ||
            lowerUrl.endsWith(".aac") ||
            lowerUrl.endsWith(".ogg")
}
