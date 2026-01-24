package com.example.coccoc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.coccoc.domain.model.Article
import com.example.coccoc.ui.component.ArticleDetailScreen
import com.example.coccoc.ui.screen.ArticleListScreen
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavigationRoute.ArticleList.route
    ) {
        composable(NavigationRoute.ArticleList.route) {
            ArticleListScreen(
                onArticleClick = { article ->
                    navController.navigate(NavigationRoute.ArticleDetail.createRoute(article))
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
                Article(
                    id = "",
                    title = "Error",
                    description = "Failed to load article: $e",
                    url = ""
                )
            }

            ArticleDetailScreen(
                article = article,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
