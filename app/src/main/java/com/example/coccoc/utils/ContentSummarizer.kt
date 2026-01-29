package com.example.coccoc.utils

import com.example.coccoc.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ContentSummarizer {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash-lite",
            apiKey = apiKey
        )
    }

    suspend fun summarizeText(content: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "null") {
            return@withContext "Please set GEMINI_API_KEY in your local.properties file."
        }

        try {
            // Reduce max chars to avoid API errors (roughly 4 chars per token)
            // Gemini Pro can handle ~30k tokens input, but keeping it conservative
            val maxChars = 4000

            // Clean and limit content
            val cleanContent = content
                .replace(Regex("\\s+"), " ") // Normalize whitespace
                .replace(Regex("[^\\p{L}\\p{N}\\s.,!?;:'\"-]"), "") // Remove special chars
                .trim()
                .take(maxChars)

            if (cleanContent.length < 50) {
                return@withContext "Content too short to summarize"
            }

            Timber.d("Sending ${cleanContent.length} characters to Gemini API")

            val prompt = """
                Summarize the following Vietnamese article concisely in 3-4 sentences.
                Focus on the main points and key information.
                
                Article:
                $cleanContent
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)

            val summary = response.text?.trim() ?: "Unable to generate summary."
            Timber.d("Gemini AI Summary: $summary")

            return@withContext summary
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown error"
            Timber.e(e, "Error calling Gemini API: $errorMsg")

            return@withContext when {
                errorMsg.contains("API key", ignoreCase = true) ->
                    "Invalid API key. Get a free key at: https://aistudio.google.com/apikey"
                errorMsg.contains("quota", ignoreCase = true) ->
                    "API quota exceeded. Try again later."
                errorMsg.contains("429") ->
                    "Rate limit reached. Please wait a moment."
                errorMsg.contains("not found", ignoreCase = true) || errorMsg.contains("NOT_FOUND") ->
                    "Model not available. Try using 'gemini-1.5-flash' model instead."
                errorMsg.contains("MissingFieldException") || errorMsg.contains("response error", ignoreCase = true) ->
                    "Content too large or API issue. Try with a shorter article."
                errorMsg.contains("blocked", ignoreCase = true) || errorMsg.contains("safety", ignoreCase = true) ->
                    "Content blocked by safety filters."
                else -> "Error: $errorMsg"
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
