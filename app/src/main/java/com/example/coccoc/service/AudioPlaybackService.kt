package com.example.coccoc.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val audioUrl: String = "",
    val title: String = ""
)

@AndroidEntryPoint
class AudioPlaybackService : MediaSessionService() {
    private lateinit var exoPlayer: ExoPlayer
    private var mediaSession: MediaSession? = null
    private var mediaController: MediaController? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var positionUpdateJob: Job? = null
    private val _playbackState = MutableStateFlow(AudioPlaybackState())

    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private val binder = AudioPlaybackBinder()

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlaybackState(isPlaying = isPlaying)
                    if (isPlaying) {
                        startPositionUpdates()
                    } else {
                        stopPositionUpdates()
                    }
                    Timber.d("Playback state changed: $isPlaying")
                }

                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_READY -> {
                            updatePlaybackState(duration = exoPlayer.duration)
                            Timber.d("Player ready, duration: ${exoPlayer.duration}")
                        }
                        Player.STATE_ENDED -> {
                            stopPositionUpdates()
                            Timber.d("Playback ended")
                        }
                        else -> {}
                    }
                }
            })
        }

        // Create MediaSession
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setCallback(object : MediaSession.Callback {})
            .build()

        // Create MediaController for this session
        mediaController = MediaController.Builder(this, mediaSession!!.token).buildAsync().get()

        Timber.d("AudioPlaybackService created")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onBind(intent: Intent?): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        stopPositionUpdates()
        mediaController?.release()
        mediaController = null
        mediaSession?.release()
        mediaSession = null
        exoPlayer.release()
        serviceScope.launch {
            serviceScope.coroutineContext[Job]?.cancel()
        }
        Timber.d("AudioPlaybackService destroyed")
        super.onDestroy()
    }

    fun prepareAndPlay(audioUrl: String, title: String) {
        try {
            Timber.d("prepareAndPlay called with URL: $audioUrl, title: $title")
            val mediaItem = MediaItem.Builder()
                .setUri(audioUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .build()
                )
                .build()
            mediaController?.apply {
                setMediaItem(mediaItem)
                prepare()
                play()
            }
            updatePlaybackState(audioUrl = audioUrl, title = title, isPlaying = true)
            Timber.d("Prepared and playing: $title")
        } catch (e: Exception) {
            Timber.e(e, "Error preparing audio: ${e.message}")
        }
    }

    fun pause() {
        mediaController?.pause()
        updatePlaybackState(isPlaying = false)
        Timber.d("Playback paused")
    }

    fun resume() {
        mediaController?.play()
        updatePlaybackState(isPlaying = true)
        Timber.d("Playback resumed")
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        Timber.d("Seeking to: $position")
    }

    fun getCurrentPosition(): Long = mediaController?.currentPosition ?: 0L

    fun getDuration(): Long = mediaController?.duration ?: 0L

    fun isPlaying(): Boolean = mediaController?.isPlaying ?: false

    private fun startPositionUpdates() {
        positionUpdateJob = serviceScope.launch {
            while (mediaController?.isPlaying == true) {
                updatePlaybackState(currentPosition = mediaController?.currentPosition ?: 0L)
                delay(200) // Update every 200ms
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private fun updatePlaybackState(
        isPlaying: Boolean = _playbackState.value.isPlaying,
        currentPosition: Long = _playbackState.value.currentPosition,
        duration: Long = _playbackState.value.duration,
        audioUrl: String = _playbackState.value.audioUrl,
        title: String = _playbackState.value.title
    ) {
        _playbackState.value = AudioPlaybackState(
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            audioUrl = audioUrl,
            title = title
        )
    }

    inner class AudioPlaybackBinder : Binder() {
        fun getService(): AudioPlaybackService = this@AudioPlaybackService
    }
}
