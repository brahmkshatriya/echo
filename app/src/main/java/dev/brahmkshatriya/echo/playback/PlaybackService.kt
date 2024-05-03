package dev.brahmkshatriya.echo.playback

import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.CLOSE_PLAYER
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {
    @Inject
    lateinit var extensionFlow: ExtensionModule.ExtensionFlow

    @Inject
    lateinit var extensionList: ExtensionModule.ExtensionListFlow

    @Inject
    lateinit var global: Queue

    @Inject
    lateinit var throwableFlow: MutableSharedFlow<Throwable>

    @Inject
    lateinit var messageFlow: MutableSharedFlow<SnackBar.Message>

    @Inject
    lateinit var settings: SharedPreferences

    @Inject
    lateinit var cache: SimpleCache

    private var mediaLibrarySession: MediaLibrarySession? = null
    private val scope = CoroutineScope(Dispatchers.IO) + Job()


    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val streamableFactory = StreamableDataSource.Factory(this)
        val cacheFactory = CacheDataSource
            .Factory().setCache(cache)
            .setUpstreamDataSourceFactory(streamableFactory)
        val dataSourceFactory =
            ResolvingDataSource.Factory(
                cacheFactory,
                TrackResolver(this, global, extensionList, settings)
            )

        val factory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(this, factory)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(audioAttributes, true)
            .build()

        val listener = QueueListener(
            this, player, extensionList, global, scope, settings, throwableFlow, messageFlow
        )
        player.addListener(listener)

        val intent = Intent(this, MainActivity::class.java)
            .putExtra("fromNotification", true)

        val pendingIntent = PendingIntent
            .getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val callback = PlayerSessionCallback(this, scope, global, extensionList, extensionFlow)

        val mediaLibrarySession = MediaLibrarySession.Builder(this, player, callback)
            .setSessionActivity(pendingIntent)
            .setBitmapLoader(PlayerBitmapLoader(this, global, scope))
            .build()

        val notificationProvider =
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelName(R.string.app_name)
                .build()
        notificationProvider.setSmallIcon(R.drawable.ic_mono)
        setMediaNotificationProvider(notificationProvider)

        scope.launch {
            global.listenToChanges(mediaLibrarySession) {
                val supportsLike = extensionList.getClient(it.clientId) is LibraryClient
                mediaLibrarySession.setCustomLayout(
                    listOfNotNull(
                        if (supportsLike) toLikeCommand(this@PlaybackService, it.liked)
                        else null,
                    )
                )
            }
        }
        this.mediaLibrarySession = mediaLibrarySession
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