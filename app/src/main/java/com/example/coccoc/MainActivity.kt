package com.example.coccoc

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.coccoc.ui.navigation.AppNavHost
import com.example.coccoc.ui.navigation.NavigationRoute
import com.example.coccoc.ui.theme.CocCocTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var deepLinkRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            CocCocTheme {
                AppNavHost(
                    startDestination = deepLinkRoute ?: NavigationRoute.ArticleList.route,
                    onNavigationHandled = { deepLinkRoute = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.d("onNewIntent called")
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val deepLink = intent?.getStringExtra("navigate_to")
        if (deepLink != null) {
            Timber.d("Deep link received: $deepLink")
            deepLinkRoute = deepLink
        }
    }
}
