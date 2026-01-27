package com.example.coccoc.utils

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ContentSummarizer {
    private val apiKey = ""

    private val generativeModel = GenerativeModel(
        modelName = "Gemini 2.0 Flash-Lite",
        apiKey = apiKey
    )

    suspend fun summarizeText(content: String): String = withContext(Dispatchers.IO) {
        try {
            val cleanContent = content.replace(Regex("\\s+"), " ").trim()

            val prompt = "Summarize the following text concisely in 3 sentences:\n\n$cleanContent"

            val response = generativeModel.generateContent(prompt)

            val summary = response.text ?: "Unable to generate summary."
            Timber.d("Cloud AI Summary: $summary")

            return@withContext summary
        } catch (e: Exception) {
            Timber.e(e, "Error calling Gemini API")
            return@withContext "Network error or API limit reached."
        }
    }

    suspend fun summarizeHtml(htmlContent: String): String {
        // Cleaning HTML reduces token usage (saves money/quota)
        val textContent = htmlContent
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<[^>]*>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return summarizeText(textContent)
    }
}
