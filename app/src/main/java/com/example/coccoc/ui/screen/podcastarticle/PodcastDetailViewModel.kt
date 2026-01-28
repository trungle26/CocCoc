package com.example.coccoc.ui.screen.podcastarticle

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coccoc.domain.model.Article
import com.example.coccoc.domain.usecase.GetPodcastDetailUseCase
import com.example.coccoc.service.AudioPlaybackService
import com.example.coccoc.utils.audio.AudioDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PodcastDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getPodcastDetailUseCase: GetPodcastDetailUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val audioDownloadManager = AudioDownloadManager(context)
    private val _uiState = MutableStateFlow(PodcastDetailUiState())
    val uiState: StateFlow<PodcastDetailUiState> = _uiState.asStateFlow()

    private var audioPlaybackService: AudioPlaybackService? = null
    private var isBound = false
    private var pendingPodcast: Article? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioPlaybackService.AudioPlaybackBinder
            audioPlaybackService = binder.getService()
            isBound = true
            Timber.d("Service connected")

            // Subscribe to service playback state
            viewModelScope.launch {
                audioPlaybackService?.playbackState?.collect { state ->
                    _uiState.value = _uiState.value.copy(
                        isPlaying = state.isPlaying,
                        currentPosition = state.currentPosition,
                        duration = state.duration
                    )
                }
            }

            // Play pending podcast if any
            pendingPodcast?.let { podcast ->
                audioPlaybackService?.prepareAndPlay(
                    audioUrl = podcast.audioUrl ?: "",
                    title = podcast.title
                )
                Timber.d("Playing pending podcast: ${podcast.title}")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            audioPlaybackService = null
            Timber.d("Service disconnected")
        }
    }

    fun loadPodcast(podcast: Article) {
        Timber.d("podcast data: ${podcast}")
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val result = getPodcastDetailUseCase.execute(podcast)
                result.onSuccess { loadedPodcast ->
                    _uiState.value = _uiState.value.copy(
                        podcast = loadedPodcast,
                        isLoading = false,
                        error = null
                    )
                    Timber.d("Podcast loaded: ${loadedPodcast.title}")

                    // Store the podcast to play after service connection
                    pendingPodcast = loadedPodcast

                    // Bind to service if not already bound
                    if (!isBound) {
                        bindToService()
                    } else {
                        // If already bound, play immediately
                        audioPlaybackService?.prepareAndPlay(
                            audioUrl = loadedPodcast.audioUrl ?: "",
                            title = loadedPodcast.title
                        )
                    }
                }
                result.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Unknown error"
                    )
                    Timber.e(exception, "Failed to load podcast")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
                Timber.e(e, "Error loading podcast")
            }
        }
    }

    fun togglePlayPause() {
        audioPlaybackService?.apply {
            if (isPlaying()) {
                pause()
            } else {
                resume()
            }
        }
    }

    fun seekTo(position: Long) {
        audioPlaybackService?.seekTo(position)
    }

    fun downloadPodcast(showToast: (String) -> Unit) {
        if (_uiState.value.isDownloading) return

        val podcast = _uiState.value.podcast
        val audioUrl = podcast?.audioUrl

        if (audioUrl.isNullOrEmpty()) {
            _uiState.value = _uiState.value.copy(
                message = "No audio URL available"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isDownloading = true)
        viewModelScope.launch {
            try {
                val fileName = audioDownloadManager.getFileNameFromUrl(audioUrl)

                val result = audioDownloadManager.downloadAudio(audioUrl, fileName)
                if (result == -1L) {
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        message = "Download failed"
                    )
                    Timber.e("Failed to download podcast")
                } else {
                    // Get download directory path
                    val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    ).absolutePath
                    val fullPath = "$downloadDir/$fileName"

                    showToast("Downloaded to: $fullPath")
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        message = "Podcast downloaded successfully"
                    )
                    Timber.d("Podcast downloaded: $fullPath")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    message = "Error: ${e.message}"
                )
                Timber.e(e, "Error downloading podcast")
            }
        }
    }

    private fun bindToService() {
        val intent = Intent(context, AudioPlaybackService::class.java)
        // Start the service first to ensure it keeps running
        context.startService(intent)
        // Then bind to it
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        Timber.d("Service start and bind initiated")
    }

    override fun onCleared() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
        Timber.d("PodcastDetailViewModel cleared")
        super.onCleared()
    }
}
