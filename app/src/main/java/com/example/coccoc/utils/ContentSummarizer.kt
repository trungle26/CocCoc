package com.example.coccoc.utils

import android.content.Context
import com.example.coccoc.BuildConfig
import com.example.coccoc.R
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ContentSummarizer(private val context: Context) {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash-lite",
            apiKey = apiKey
        )
    }

    private fun isVietnamese(): Boolean {
        val locale = context.resources.configuration.locales[0]
        return locale.language == "vi"
    }


    private fun getString(resId: Int, vararg args: Any): String {
        return context.getString(resId, *args)
    }

    suspend fun summarizeText(content: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "null") {
            return@withContext getString(R.string.api_key_required)
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
                return@withContext getString(R.string.content_too_short)
            }

            Timber.d("Sending ${cleanContent.length} characters to Gemini API")

            // Create prompt based on current language
            val isVi = isVietnamese()
            val prompt = if (isVi) {
                """
                Hãy tóm tắt bài báo tiếng Việt sau đây một cách ngắn gọn trong 3-4 câu.
                Tập trung vào các điểm chính và thông tin quan trọng.
                
                Bài báo:
                $cleanContent
            """.trimIndent()
            } else {
                """
                Summarize the following Vietnamese article concisely in 3-4 sentences in English.
                Focus on the main points and key information.
                
                Article:
                $cleanContent
            """.trimIndent()
            }

            val response = generativeModel.generateContent(prompt)

            val summary = response.text?.trim() ?: "Unable to generate summary."
            Timber.d("Gemini AI Summary: $summary")

            return@withContext summary
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown error"
            Timber.e(e, "Error calling Gemini API: $errorMsg")

            return@withContext when {
                errorMsg.contains("API key", ignoreCase = true) ->
                    getString(R.string.invalid_api_key)
                errorMsg.contains("quota", ignoreCase = true) ->
                    getString(R.string.api_quota_exceeded)
                errorMsg.contains("429") ->
                    getString(R.string.rate_limit_reached)
                errorMsg.contains("not found", ignoreCase = true) || errorMsg.contains("NOT_FOUND") ->
                    getString(R.string.model_not_available)
                errorMsg.contains("MissingFieldException") || errorMsg.contains("response error", ignoreCase = true) ->
                    getString(R.string.content_too_large)
                errorMsg.contains("blocked", ignoreCase = true) || errorMsg.contains("safety", ignoreCase = true) ->
                    getString(R.string.content_blocked)
                else -> getString(R.string.error_summarizing, errorMsg)
            }
        }
    }
}
