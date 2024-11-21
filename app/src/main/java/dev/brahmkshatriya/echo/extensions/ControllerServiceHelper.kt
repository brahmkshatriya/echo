package dev.brahmkshatriya.echo.extensions

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.brahmkshatriya.echo.common.clients.ControllerClient.RepeatMode


@UnstableApi
class ControllerServiceHelper(private val parentService: Service) {
    private var mediaService: ControllerExtensionService? = null
    private var isServiceBound = false
    private var player: Player? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ControllerExtensionService.LocalBinder
            mediaService = binder.getService()
            isServiceBound = true
            player?.let { mediaService?.setPlayer(it) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaService = null
            isServiceBound = false
        }
    }

    fun startService(player: Player) {
        this.player = player

        val intent = Intent(parentService, ControllerExtensionService::class.java).apply {
            action = ControllerExtensionService.ACTION_START_SERVICE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            parentService.startForegroundService(intent)
        } else {
            parentService.startService(intent)
        }

        parentService.bindService(
            Intent(parentService, ControllerExtensionService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun stopService() {
        if (isServiceBound) {
            parentService.unbindService(serviceConnection)
            isServiceBound = false
        }
        parentService.stopService(Intent(parentService, ControllerExtensionService::class.java))
        player = null
    }
    fun play() = mediaService?.play()
    fun pause() = mediaService?.pause()
    fun seekToNext() = mediaService?.seekToNext()
    fun seekToPrevious() = mediaService?.seekToPrevious()
    fun seekTo(position: Long) = mediaService?.seekTo(position)
    fun seekToMediaItem(index: Int) = mediaService?.seekToMediaItem(index)
    fun moveMediaItem(fromIndex: Int, toIndex: Int) =
        mediaService?.moveMediaItem(fromIndex, toIndex)
    fun removeMediaItem(index: Int) = mediaService?.removeMediaItem(index)
    fun setShuffleMode(enabled: Boolean) = mediaService?.setShuffleMode(enabled)
    fun setRepeatMode(repeatMode: RepeatMode) = mediaService?.setRepeatMode(repeatMode)
}