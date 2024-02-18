package dev.brahmkshatriya.echo.player

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.data.clients.ExtensionClient
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject
    lateinit var app: Application

    @Inject
    lateinit var extension: ExtensionClient

    private var mediaLibrarySession: MediaLibrarySession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(audioAttributes, true)
            .build()

        val intent = Intent(this, MainActivity::class.java)
            .putExtra("fromNotification", true)

        val pendingIntent = PendingIntent
            .getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, PlayerSessionCallback(app, extension))
            .setSessionActivity(pendingIntent)
            .build()

        val notificationProvider = DefaultMediaNotificationProvider
            .Builder(this)
            .setChannelName(R.string.app_name)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_mono)

        setMediaNotificationProvider(notificationProvider)
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaLibrarySession

}