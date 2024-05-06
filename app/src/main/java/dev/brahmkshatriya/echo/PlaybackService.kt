package dev.brahmkshatriya.echo

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
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.playback.PlayerBitmapLoader
import dev.brahmkshatriya.echo.playback.PlayerListener
import dev.brahmkshatriya.echo.playback.PlayerSessionCallback
import dev.brahmkshatriya.echo.playback.Queue
import dev.brahmkshatriya.echo.playback.RenderersFactory
import dev.brahmkshatriya.echo.playback.StreamableDataSource
import dev.brahmkshatriya.echo.playback.TrackResolver
import dev.brahmkshatriya.echo.playback.getLikeButton
import dev.brahmkshatriya.echo.playback.getRepeatButton
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.getClient
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.CLOSE_PLAYER
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.SKIP_SILENCE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {
    @Inject
    lateinit var extensionFlow: MutableStateFlow<MusicExtension?>

    @Inject
    lateinit var extensionList: MutableStateFlow<List<MusicExtension>?>

    @Inject
    lateinit var global: Queue

    @Inject
    lateinit var settings: SharedPreferences

    @Inject
    lateinit var cache: SimpleCache

    @Inject
    lateinit var listener: PlayerListener

    private var mediaLibrarySession: MediaLibrarySession? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        val player = createExoplayer()
        listener.setup(player, scope)

        val intent = Intent(this, MainActivity::class.java)
            .putExtra("fromNotification", true)

        val pendingIntent = PendingIntent
            .getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val callback = PlayerSessionCallback(
            this, scope, global, extensionList, extensionFlow
        )

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

        global.listenToChanges(scope, mediaLibrarySession, ::updateLayout)
        settings.registerOnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                SKIP_SILENCE -> player.skipSilenceEnabled = prefs.getBoolean(key, true)
            }
        }
        this.mediaLibrarySession = mediaLibrarySession
    }


    private fun createExoplayer() = run {
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

        ExoPlayer.Builder(this, factory)
            .setRenderersFactory(RenderersFactory(this))
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSkipSilenceEnabled(settings.getBoolean(SKIP_SILENCE, true))
            .setAudioAttributes(audioAttributes, true)
            .build()
    }

    private fun updateLayout() {
        val context = this@PlaybackService
        val track = global.current ?: return
        val mediaLibrarySession = mediaLibrarySession ?: return
        val player = mediaLibrarySession.player
        val supportsLike = extensionList.getClient(track.clientId)?.client is LibraryClient

        val commandButtons = listOfNotNull(
            getRepeatButton(context, player.repeatMode),
            getLikeButton(context, track.liked).takeIf { supportsLike }
        )
        mediaLibrarySession.setCustomLayout(commandButtons)
    }

    override fun onDestroy() {
        scope.launch { global.clearQueue(false) }
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val stopPlayer = settings.getBoolean(CLOSE_PLAYER, false)
        val isPlaying = mediaLibrarySession?.player?.isPlaying ?: false
        if (!isPlaying || stopPlayer) stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaLibrarySession
}