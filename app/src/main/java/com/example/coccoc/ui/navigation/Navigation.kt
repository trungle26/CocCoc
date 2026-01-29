package com.example.coccoc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.coccoc.domain.model.Article
import com.example.coccoc.ui.screen.normalarticle.ArticleDetailScreen
import com.example.coccoc.ui.screen.ArticleListScreen
import com.example.coccoc.ui.screen.podcastarticle.PodcastDetailScreen
import com.example.coccoc.domain.model.Constants
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.net.URLEncoder
import java.net.URLDecoder

sealed class NavigationRoute(val route: String) {
    object ArticleList : NavigationRoute("article_list")
    object ArticleDetail : NavigationRoute("article_detail/{article}") {
        fun createRoute(article: Article): String {
            val json = Json.encodeToString(article)
            val encoded = URLEncoder.encode(json, "UTF-8")
            return "article_detail/$encoded"
        }
    }

    object PodcastDetail : NavigationRoute("podcast_detail/{article}") {
        fun createRoute(article: Article): String {
            val json = Json.encodeToString(article)
            val encoded = URLEncoder.encode(json, "UTF-8")
            return "podcast_detail/$encoded"
        }
    }
}

@Composable
fun AppNavHost(
    modifier : Modifier = Modifier,
    startDestination: String = NavigationRoute.ArticleList.route,
    onNavigationHandled: () -> Unit = {}
) {
    val navController = rememberNavController()

    LaunchedEffect(startDestination) {
        if (startDestination != NavigationRoute.ArticleList.route) {
            Timber.d("Navigating to deep link: $startDestination")

            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != startDestination) {
                navController.navigate(startDestination) {
                    launchSingleTop = true
                }
            }
            onNavigationHandled()
        }
    }

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = NavigationRoute.ArticleList.route
    ) {
        composable(NavigationRoute.ArticleList.route) {
            ArticleListScreen(
                onArticleClick = { article ->
                    if (article.type == "podcast") {
                        navController.navigate(NavigationRoute.PodcastDetail.createRoute(article))
                    } else {
                        navController.navigate(NavigationRoute.ArticleDetail.createRoute(article))
                    }
                }
            )
        }

        composable(
            route = NavigationRoute.ArticleDetail.route,
            arguments = listOf(
                navArgument("article") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val articleJson = backStackEntry.arguments?.getString("article") ?: ""
            val article = try {
                val decoded = URLDecoder.decode(articleJson, "UTF-8")
                Json.decodeFromString<Article>(decoded)
            } catch (e: Exception) {
                Timber.e(e, "Failed to decode article JSON for ArticleDetail")
                Article(
                    title = "Error Loading Article",
                    link = "",
                    description = "Failed to load article: ${e.message}",
                    pubDate = "",
                    imageUrl = null
                )
            }
            ArticleDetailScreen(article = article, onBackClick = {navController.popBackStack()})
        }

        composable(
            route = NavigationRoute.PodcastDetail.route,
            arguments = listOf(
                navArgument(Constants.NAV_ARG_ARTICLE) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val articleJson = backStackEntry.arguments?.getString(Constants.NAV_ARG_ARTICLE) ?: ""
            val article = try {
                val decoded = URLDecoder.decode(articleJson, Constants.ENCODING_UTF8)
                Json.decodeFromString<Article>(decoded)
            } catch (e: Exception) {
                Timber.e(e, "Failed to decode article JSON for PodcastDetail")
                Article(
                    title = "Error Loading Podcast",
                    link = "",
                    description = "Failed to load podcast: ${e.message}",
                    pubDate = "",
                    imageUrl = null,
                    audioUrl = null, // Explicitly set to null so validation catches it
                    type = Constants.ARTICLE_TYPE_PODCAST
                )
            }

            PodcastDetailScreen(
                podcast = article,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
