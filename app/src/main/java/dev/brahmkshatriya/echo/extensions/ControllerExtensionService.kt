package dev.brahmkshatriya.echo.extensions

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import dev.brahmkshatriya.echo.R
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.Player
import dev.brahmkshatriya.echo.common.clients.ControllerClient.RepeatMode

@UnstableApi
class ControllerExtensionService : Service() {
    private val binder = LocalBinder()
    private var player: Player? = null

    inner class LocalBinder : Binder() {
        fun getService(): ControllerExtensionService = this@ControllerExtensionService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.media_playback_controller),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.media_playback_controller_description)
                setSound(null, null)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.media_playback_controller))
            .setContentText(getString(R.string.media_playback_controller_running))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    fun setPlayer(exoPlayer: Player) {
        player = exoPlayer
    }

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun seekToNext() {
        player?.seekToNextMediaItem()
    }

    fun seekToPrevious() {
        player?.seekToPreviousMediaItem()
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun seekToMediaItem(index: Int) {
        player?.seekTo(index, 0)
    }

    fun moveMediaItem(fromIndex: Int, toIndex: Int) {
        player?.moveMediaItem(fromIndex, toIndex)
    }

    fun removeMediaItem(index: Int) {
        player?.removeMediaItem(index)
    }

    fun setShuffleMode(enabled: Boolean) {
        player?.shuffleModeEnabled = enabled
    }

    fun setRepeatMode(repeatMode: RepeatMode) {
        player?.repeatMode = repeatMode.ordinal
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_DETACH)
        player = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "media_playback_channel"
        const val ACTION_START_SERVICE = "action.START_SERVICE"
    }
}