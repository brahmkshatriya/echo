package dev.brahmkshatriya.echo.player

import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.di.ExtensionFlow
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.Companion.CLOSE_PLAYER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {
    @Inject
    lateinit var extension: ExtensionFlow

    @Inject
    lateinit var global: Queue

    @Inject
    lateinit var throwableFlow: MutableSharedFlow<Throwable>

    @Inject
    lateinit var settings: SharedPreferences

    private var mediaLibrarySession: MediaLibrarySession? = null
    private val scope = CoroutineScope(Dispatchers.IO) + Job()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build()

        val customDataSource = CustomDataSource.Factory(this, global, throwableFlow)
        val factory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(customDataSource)

        val player = ExoPlayer.Builder(this, factory)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(audioAttributes, true)
            .build()

        player.addListener(RadioListener(player, global, scope, settings))

        val intent = Intent(this, MainActivity::class.java).putExtra("fromNotification", true)

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        mediaLibrarySession = MediaLibrarySession.Builder(
            this, player, PlayerSessionCallback(this, scope, global, extension.flow)
        ).setSessionActivity(pendingIntent).build()

        val notificationProvider =
            DefaultMediaNotificationProvider.Builder(this).setChannelName(R.string.app_name).build()
        notificationProvider.setSmallIcon(R.drawable.ic_mono)

        setMediaNotificationProvider(notificationProvider)
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            scope.launch {
                global.clearQueue()
                release()
                mediaLibrarySession = null
            }
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaLibrarySession?.run {
            val stopPlayer = settings.getBoolean(CLOSE_PLAYER, false)
            if (!player.isPlaying || stopPlayer) {
                onDestroy()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaLibrarySession
}


