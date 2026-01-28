package com.example.coccoc.utils.audio

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import java.io.File

class AudioDownloadManager(private val context: Context) {
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

    private fun getAudioFile(fileName: String): File? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val audioFile = File(downloadsDir, fileName)
        return if (audioFile.exists()) audioFile else null
    }

    fun deleteAudio(fileName: String) {
        val audioFile = getAudioFile(fileName)
        audioFile?.delete()
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
