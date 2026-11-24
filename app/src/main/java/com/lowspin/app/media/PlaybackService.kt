package com.lowspin.app.media

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.lowspin.app.MainActivity
import com.lowspin.app.LowspinApplication
import com.lowspin.app.R
import com.lowspin.app.data.db.PlaybackStateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val smartShuffle = SmartShuffleManager()
    private var persistJob: Job? = null

    private val container: com.lowspin.app.di.AppContainer
        get() = (application as LowspinApplication).container

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player.currentMediaItemIndex
                if (smartShuffle.shouldSkipImmediateRepeat(
                        nextIndex = index,
                        reason = reason,
                        shuffleEnabled = player.shuffleModeEnabled,
                        repeatMode = player.repeatMode,
                        queueSize = player.mediaItemCount
                    )
                ) {
                    player.seekToNextMediaItem()
                    return
                }
                smartShuffle.onPlayed(index)
                persistProgress()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) persistProgress()
            }
        })

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this).build()
        notificationProvider.setSmallIcon(R.drawable.ic_stat_vinyl)
        setMediaNotificationProvider(notificationProvider)

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setCallback(SessionCallback())
            .build()

        startPersistLoop()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        persistJob?.cancel()
        runCatching { runBlocking { saveProgress() } }
        mediaSession?.release()
        mediaSession = null
        if (::player.isInitialized) player.release()
        scope.cancel()
        super.onDestroy()
    }

    private fun startPersistLoop() {
        persistJob = scope.launch {
            while (isActive) {
                delay(5_000)
                if (::player.isInitialized && player.isPlaying) persistProgress()
            }
        }
    }

    private fun persistProgress() {
        scope.launch { saveProgress() }
    }

    private suspend fun saveProgress() {
        if (!::player.isInitialized || player.mediaItemCount == 0) return
        val index = player.currentMediaItemIndex
        if (index < 0 || player.currentMediaItem == null) return

        val position = player.currentPosition
        val shuffle = player.shuffleModeEnabled
        val repeat = player.repeatMode
        val speed = player.playbackParameters.speed

        val db = container.database
        val existing = db.playbackStateDao().get() ?: PlaybackStateEntity()
        db.playbackStateDao().upsert(
            existing.copy(
                currentQueuePosition = index,
                positionMs = position,
                shuffleEnabled = shuffle,
                repeatMode = repeat,
                playbackSpeed = speed,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun buildResumption(): MediaSession.MediaItemsWithStartPosition {
        val db = container.database
        val queue = db.queueDao().getAll()
        check(queue.isNotEmpty()) { "No saved queue" }

        val songs = db.songDao().getByIds(queue.map { it.songId }).associateBy { it.id }
        val ordered = queue.mapNotNull { songs[it.songId] }
        check(ordered.isNotEmpty()) { "Saved queue has no playable songs" }

        val saved = db.playbackStateDao().get() ?: PlaybackStateEntity()
        return MediaSession.MediaItemsWithStartPosition(
            ordered.map { it.toMediaItem() },
            saved.currentQueuePosition.coerceIn(0, ordered.lastIndex),
            saved.positionMs.coerceAtLeast(0L)
        )
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controllerInfo: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            scope.launch {
                runCatching { buildResumption() }
                    .onSuccess { future.set(it) }
                    .onFailure { future.setException(it) }
            }
            return future
        }
    }
}
