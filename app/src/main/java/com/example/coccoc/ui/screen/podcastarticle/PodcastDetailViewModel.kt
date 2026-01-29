package com.example.coccoc.ui.screen.podcastarticle

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Environment
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.coccoc.domain.model.Article
import com.example.coccoc.service.AudioPlaybackService
import com.example.coccoc.utils.AudioDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class PodcastDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val audioDownloadManager = AudioDownloadManager(context)
    private val _uiState = MutableStateFlow<PodcastDetailUiState>(PodcastDetailUiState.Loading)
    val uiState: StateFlow<PodcastDetailUiState> = _uiState.asStateFlow()

    private var serviceBinder: AudioPlaybackService.AudioPlaybackBinder? = null
    private var isBound = false
    private var pendingPodcast: Article? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioPlaybackService.AudioPlaybackBinder
            serviceBinder = binder
            isBound = true
            Timber.d("Service connected")

            viewModelScope.launch {
                binder.getService().playbackState.collect { audioPlaybackState ->
                    _uiState.update { currentState ->
                        if (currentState is PodcastDetailUiState.Success) {
                            currentState.copy(
                                playbackState = currentState.playbackState.copy(
                                    isPlaying = audioPlaybackState.isPlaying,
                                    currentPosition = audioPlaybackState.currentPosition,
                                    duration = audioPlaybackState.duration
                                )
                            )
                        } else currentState
                    }
                }
            }

            pendingPodcast?.let { podcast ->
                val currentAudioUrl = binder.getService().playbackState.value.audioUrl

                if (currentAudioUrl != podcast.audioUrl) {
                    Timber.d("Service connected, playing pending podcast: ${podcast.title}")
                    binder.getService().prepareAndPlay(
                        audioUrl = podcast.audioUrl ?: "",
                        title = podcast.title,
                        article = podcast
                    )
                } else {
                    Timber.d("Service already playing this podcast, not reloading")
                }
                pendingPodcast = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            serviceBinder = null
            Timber.d("Service disconnected")
        }
    }

    fun loadPodcast(podcast: Article) {
        Timber.d("loadPodcast called for: ${podcast.title}")

        val currentState = _uiState.value
        if (currentState is PodcastDetailUiState.Success &&
            currentState.podcast.audioUrl == podcast.audioUrl) {
            Timber.d("Podcast already loaded, skipping reload")
            return
        }

        if (podcast.audioUrl.isNullOrBlank()) {
            Timber.e("Podcast has no audio URL: ${podcast.title}")
            _uiState.value = PodcastDetailUiState.Error(
                errorMessage = "This podcast has no audio available"
            )
            return
        }

        _uiState.value = PodcastDetailUiState.Success(podcast = podcast)
        Timber.d("Podcast loaded: ${podcast.title}, audioUrl: ${podcast.audioUrl}")

        pendingPodcast = podcast

        if (!isBound) {
            Timber.d("Service not bound, binding to service")
            bindToService()
        } else {
            val service = serviceBinder?.getService()
            val currentAudioUrl = service?.playbackState?.value?.audioUrl

            if (currentAudioUrl != podcast.audioUrl) {
                Timber.d("Service bound but different podcast, playing new podcast")
                service?.prepareAndPlay(
                    audioUrl = podcast.audioUrl,
                    title = podcast.title,
                    article = podcast
                )
            } else {
                Timber.d("Service already playing this podcast, not reloading")
            }
        }
    }

    fun togglePlayPause() {
        serviceBinder?.getService()?.apply {
            if (isPlaying()) {
                pause()
            } else {
                resume()
            }
        }
    }

    fun seekTo(position: Long) {
        serviceBinder?.getService()?.seekTo(position)
    }

    fun clearMessage() {
        _uiState.update { currentState ->
            when (currentState) {
                is PodcastDetailUiState.Success -> currentState.copy(message = null)
                else -> currentState
            }
        }
    }

    fun downloadPodcast() {
        val currentState = _uiState.value
        if (currentState !is PodcastDetailUiState.Success) return

        val podcast = currentState.podcast
        val audioUrl = podcast.audioUrl

        if (audioUrl.isNullOrEmpty()) {
            _uiState.update { state ->
                if (state is PodcastDetailUiState.Success) {
                    state.copy(message = "No audio URL available")
                } else state
            }
            return
        }

        _uiState.value = currentState.copy(isDownloading = true)

        viewModelScope.launch {
            try {
                val fileName = audioDownloadManager.getFileNameFromUrl(audioUrl)

                val result = audioDownloadManager.downloadAudio(audioUrl, fileName)
                if (result == -1L) {
                    _uiState.update { state ->
                        if (state is PodcastDetailUiState.Success) {
                            state.copy(
                                isDownloading = false,
                                message = "Download failed"
                            )
                        } else state
                    }
                    Timber.e("Failed to download podcast")
                } else {
                    val downloadDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    ).absolutePath
                    val fullPath = "$downloadDir/$fileName"

                    _uiState.update { state ->
                        if (state is PodcastDetailUiState.Success) {
                            state.copy(
                                isDownloading = false,
                                message = "Added to download queue: $fullPath"
                            )
                        } else state
                    }
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    if (state is PodcastDetailUiState.Success) {
                        state.copy(
                            isDownloading = false,
                            message = "Error: ${e.message}"
                        )
                    } else state
                }
                Timber.e(e, "Error downloading podcast")
            }
        }
    }

    private fun bindToService() {
        val intent = Intent(context, AudioPlaybackService::class.java)
        context.startService(intent)
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
