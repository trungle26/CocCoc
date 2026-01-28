package com.example.coccoc.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
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
import com.example.coccoc.MainActivity
import com.example.coccoc.R
import com.example.coccoc.domain.model.Article
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder

@UnstableApi
@AndroidEntryPoint
class AudioPlaybackService : MediaSessionService() {
    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var playerNotificationManager: PlayerNotificationManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var positionUpdateJob: Job? = null
    private val _playbackState = MutableStateFlow(AudioPlaybackState())

    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private val binder = AudioPlaybackBinder()

    private var currentArticle: Article? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "audio_playback_channel"
        private const val CHANNEL_NAME = "Audio Playback"
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

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
                            updatePlaybackState(duration = this@apply.duration)
                            Timber.d("Player ready, duration: ${this@apply.duration}")
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

        // Create MediaSession with the player
        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>
                ): ListenableFuture<MutableList<MediaItem>> {
                    return Futures.immediateFuture(mediaItems)
                }
            })
            .build()

        // Setup PlayerNotificationManager
        setupPlayerNotificationManager()

        Timber.d("AudioPlaybackService created")
    }

    private fun setupPlayerNotificationManager() {
        playerNotificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            CHANNEL_ID
        )
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return player.mediaMetadata.title ?: "Unknown"
                }

                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    val intent = Intent(this@AudioPlaybackService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                        action = Intent.ACTION_MAIN
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        // Add deep link data to navigate to podcast detail
                        currentArticle?.let { article ->
                            val json = Json.encodeToString(article)
                            val encoded = URLEncoder.encode(json, "UTF-8")
                            putExtra("navigate_to", "podcast_detail/$encoded")
                        }
                    }
                    return PendingIntent.getActivity(
                        this@AudioPlaybackService,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }

                override fun getCurrentContentText(player: Player): CharSequence? {
                    return player.mediaMetadata.artist ?: "CocCoc Podcast"
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): Bitmap? {
                    // Load image asynchronously from article's imageUrl
                    currentArticle?.imageUrl?.let { imageUrl ->
                        Glide.with(this@AudioPlaybackService)
                            .asBitmap()
                            .load(imageUrl)
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap>?
                                ) {
                                    callback.onBitmap(resource)
                                }

                                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                                    // Optional: handle cleanup
                                }

                                override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                                    super.onLoadFailed(errorDrawable)
                                    Timber.w("Failed to load notification image: $imageUrl")
                                }
                            })
                    }
                    return null // Return null initially, image will be set via callback
                }
            })
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }

                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: android.app.Notification,
                    ongoing: Boolean
                ) {
                    if (ongoing) {
                        startForeground(notificationId, notification)
                    } else {
                        stopForeground(STOP_FOREGROUND_DETACH)
                    }
                }
            })
            .setSmallIconResourceId(R.drawable.ic_launcher_foreground)
            .build()

        playerNotificationManager?.apply {
            setPlayer(exoPlayer)
            setMediaSessionToken(mediaSession!!.sessionCompatToken)
            setUsePlayPauseActions(true)
            setUseNextAction(false)
            setUsePreviousAction(false)
            setUseStopAction(true)
        }
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
        playerNotificationManager?.setPlayer(null)
        playerNotificationManager = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        exoPlayer = null
        serviceScope.launch {
            serviceScope.coroutineContext[Job]?.cancel()
        }
        Timber.d("AudioPlaybackService destroyed")
        super.onDestroy()
    }

    fun prepareAndPlay(audioUrl: String, title: String, article: Article? = null) {
        try {
            Timber.d("prepareAndPlay called with URL: $audioUrl, title: $title")
            currentArticle = article

            val metadataBuilder = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist("CocCoc Podcast")
                .setDisplayTitle(title)
                .setIsPlayable(true)

            article?.imageUrl?.let { imageUrl ->
                metadataBuilder.setArtworkUri(Uri.parse(imageUrl))
            }

            val mediaItem = MediaItem.Builder()
                .setUri(audioUrl)
                .setMediaMetadata(metadataBuilder.build())
                .build()

            exoPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
            updatePlaybackState(audioUrl = audioUrl, title = title, isPlaying = true, article = article)
            Timber.d("Prepared and playing: $title")
        } catch (e: Exception) {
            Timber.e(e, "Error preparing audio: ${e.message}")
        }
    }

    fun pause() {
        exoPlayer?.pause()
        updatePlaybackState(isPlaying = false)
        Timber.d("Playback paused")
    }

    fun resume() {
        exoPlayer?.play()
        updatePlaybackState(isPlaying = true)
        Timber.d("Playback resumed")
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
        Timber.d("Seeking to: $position")
    }

    fun isPlaying(): Boolean = exoPlayer?.isPlaying ?: false

    private fun startPositionUpdates() {
        positionUpdateJob = serviceScope.launch {
            while (exoPlayer?.isPlaying == true) {
                updatePlaybackState(currentPosition = exoPlayer?.currentPosition ?: 0L)
                delay(200)
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
        title: String = _playbackState.value.title,
        article: Article? = _playbackState.value.article
    ) {
        _playbackState.value = AudioPlaybackState(
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            audioUrl = audioUrl,
            title = title,
            article = article
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification channel for audio playback"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Timber.d("Notification channel created")
        }
    }


    inner class AudioPlaybackBinder : Binder() {
        fun getService(): AudioPlaybackService = this@AudioPlaybackService
    }
}
