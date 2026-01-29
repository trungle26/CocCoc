package com.example.coccoc.utils

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri

class AudioDownloadManager(context: Context) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun downloadAudio(audioUrl: String, fileName: String): Long {
        val uri = audioUrl.toUri()
        val request = DownloadManager.Request(uri).apply {
            setTitle("Downloading Audio")
            setDescription(fileName)
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }
        return downloadManager.enqueue(request)
    }

    fun getFileNameFromUrl(url: String): String {
        return extractAudioFileName(url)
    }

    companion object {
        fun extractAudioFileName(url: String): String {
            return url.substringAfterLast("/")
        }
    }
}