package com.example.coccoc.utils

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Content Summarizer using Google Gemini AI (Free tier available)
 *
 * To get your free API key:
 * 1. Go to https://aistudio.google.com/apikey
 * 2. Sign in with your Google account
 * 3. Click "Create API key"
 * 4. Copy the key and paste it below
 *
 * Free tier limits (as of 2024):
 * - 15 requests per minute
 * - 1 million tokens per minute
 * - 1,500 requests per day
 */
class ContentSummarizer {
    // TODO: Replace with your Gemini API key from https://aistudio.google.com/apikey
    private val apiKey = "YOUR_GEMINI_API_KEY_HERE"

    // Using gemini-pro - stable and free model
    // You can also try: "gemini-1.5-flash", "gemini-1.5-pro"
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-pro",
            apiKey = apiKey
        )
    }

    suspend fun summarizeText(content: String): String = withContext(Dispatchers.IO) {
        if (apiKey == "YOUR_GEMINI_API_KEY_HERE" || apiKey.isEmpty()) {
            return@withContext "Please set your Gemini API key in ContentSummarizer.kt\nGet free key at: https://aistudio.google.com/apikey"
        }

        try {
            // Limit content to avoid token limits (roughly 4 chars per token)
            val maxChars = 10000
            val cleanContent = content
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(maxChars)

            val prompt = """
                Summarize the following article in Vietnamese. 
                Provide a concise summary in 3-4 sentences that captures the main points.
                
                Article:
                $cleanContent
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)

            val summary = response.text ?: "Unable to generate summary."
            Timber.d("Gemini AI Summary: $summary")

            return@withContext summary
        } catch (e: Exception) {
            Timber.e(e, "Error calling Gemini API: ${e.message}")
            return@withContext when {
                e.message?.contains("API key") == true -> "Invalid API key. Get a free key at: https://aistudio.google.com/apikey"
                e.message?.contains("quota") == true -> "API quota exceeded. Try again later."
                e.message?.contains("429") == true -> "Rate limit reached. Please wait a moment."
                e.message?.contains("not found") == true || e.message?.contains("NOT_FOUND") == true ->
                    "Model not available. Try updating the Gemini SDK or use a different model."
                e.message?.contains("MissingFieldException") == true ->
                    "API response error. Please check your API key and try again."
                else -> "Error: ${e.message ?: "Unknown error"}"
            }
        }
    }

    suspend fun summarizeHtml(htmlContent: String): String {
        // Clean HTML to reduce token usage
        val textContent = htmlContent
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<[^>]*>"), " ")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&[a-z]+;"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return summarizeText(textContent)
    }
}
