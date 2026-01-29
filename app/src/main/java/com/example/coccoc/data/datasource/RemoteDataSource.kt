package com.example.coccoc.data.datasource

import android.util.Xml
import com.example.coccoc.domain.model.Article
import com.example.coccoc.domain.model.Constants
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class RemoteDataSource @Inject constructor(
) {
    fun fetchArticles(): List<Article> {
        val url = URL("https://dantri.com.vn/rss/home.rss")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val articles = mutableListOf<Article>()
        connection.inputStream.use { inputStream ->
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, null)
            var eventType = parser.eventType
            var currentArticle: Article? = null
            var text = ""
            var imageUrl: String?

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName == "item") {
                            currentArticle = Article("", "", "", "", null)
                        }
                    }

                    XmlPullParser.TEXT -> text = parser.text
                    XmlPullParser.END_TAG -> {
                        if (currentArticle != null) {
                            when (tagName) {
                                "title" -> currentArticle = currentArticle.copy(title = text)
                                "link" -> currentArticle = currentArticle.copy(link = text)
                                "description" -> {
                                    // Extract image from description HTML
                                    val imgRegex = Regex("img src='([^']+)'")
                                    val descRegex = Regex("</br>(.*)")
                                    val descriptionText = descRegex.find(text)?.groupValues?.getOrNull(1)?.trim()?:""

                                    imageUrl = imgRegex.find(text)?.groupValues?.getOrNull(1)
                                    currentArticle =
                                        currentArticle.copy(description = descriptionText, imageUrl = imageUrl)
                                }

                                "pubDate" -> currentArticle = currentArticle.copy(pubDate = text)
                                "item" -> {
                                    articles.add(currentArticle)
                                    currentArticle = null
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        }
        return articles
    }

    fun fetchPodcastArticles(): List<Article> {
        val url = URL("https://vnexpress.net/rss/podcast/vnexpress-hom-nay.rss")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val podcasts = mutableListOf<Article>()
        connection.inputStream.use { inputStream ->
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, null)
            var eventType = parser.eventType
            var currentPodcast: Article? = null
            var text = ""
            var imageUrl: String? = null
            var audioUrl: String? = null
            var duration: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName == "item") {
                            currentPodcast = Article(
                                title = "",
                                link = "",
                                description = "",
                                pubDate = "",
                                imageUrl = null,
                                audioUrl = null,
                                duration = null,
                                type = Constants.ARTICLE_TYPE_PODCAST
                            )
                            imageUrl = null
                            audioUrl = null
                            duration = null
                        }
                        if (tagName == "enclosure") {
                            audioUrl = parser.getAttributeValue(null, "url")
                        }
                        // Handle itunes:image tag - check both with and without namespace
                        if (tagName == "image" && parser.namespace == "http://www.itunes.com/dtds/podcast-1.0.dtd") {
                            imageUrl = parser.getAttributeValue(null, "href")
                        } else if (tagName?.contains("image") == true) {
                            // Fallback: check if tag name contains "image" (handles itunes:image)
                            val href = parser.getAttributeValue(null, "href")
                            if (href != null) {
                                imageUrl = href
                            }
                        }
                    }
                    XmlPullParser.TEXT -> text = parser.text
                    XmlPullParser.END_TAG -> {
                        if (currentPodcast != null) {
                            when (tagName) {
                                "title" -> currentPodcast = currentPodcast.copy(title = text)
                                "link" -> currentPodcast = currentPodcast.copy(link = text)
                                "description" -> currentPodcast = currentPodcast.copy(description = text)
                                "pubDate" -> currentPodcast = currentPodcast.copy(pubDate = text)
                                "itunes:duration" -> duration = text
                                "item" -> {
                                    podcasts.add(
                                        currentPodcast.copy(
                                            imageUrl = imageUrl,
                                            audioUrl = audioUrl,
                                            duration = duration
                                        )
                                    )
                                    currentPodcast = null
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        }
        return podcasts
    }
}
