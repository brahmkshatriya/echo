package dev.brahmkshatriya.echo.playback

import android.app.Application
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.playback.listener.AudioFocusListener
import dev.brahmkshatriya.echo.playback.listener.EffectsListener
import dev.brahmkshatriya.echo.playback.listener.MediaSessionServiceListener
import dev.brahmkshatriya.echo.playback.listener.PlayerEventListener
import dev.brahmkshatriya.echo.playback.listener.PlayerRadio
import dev.brahmkshatriya.echo.playback.listener.TrackingListener
import dev.brahmkshatriya.echo.playback.renderer.PlayerBitmapLoader
import dev.brahmkshatriya.echo.playback.renderer.RenderersFactory
import dev.brahmkshatriya.echo.playback.source.StreamableMediaSource
import dev.brahmkshatriya.echo.utils.ContextUtils.listenFuture
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import org.koin.android.ext.android.inject
import java.io.File

class PlayerService : MediaLibraryService() {

    private val extensionLoader by inject<ExtensionLoader>()
    private val extensions by lazy { extensionLoader.extensions }
    private val exoPlayer by lazy { createExoplayer() }

    private var mediaSession: MediaLibrarySession? = null
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    private val app by inject<App>()
    private val state by inject<PlayerState>()
    private val scope = CoroutineScope(Dispatchers.IO) + CoroutineName("PlayerService")

    @OptIn(UnstableApi::class)
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            SKIP_SILENCE -> exoPlayer.skipSilenceEnabled = prefs.getBoolean(key, false)
        }
    }
    private val effects by lazy { EffectsListener(exoPlayer, this, state.session) }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        setListener(MediaSessionServiceListener(this, intent))

        val player = ShufflePlayer(exoPlayer)
        val callback =
            PlayerCallback(this, scope, app.throwFlow, extensions, state.radio)

        val session = MediaLibrarySession.Builder(this, player, callback)
            .setBitmapLoader(PlayerBitmapLoader(this, scope))
            .setSessionActivity(intent)
            .build()

        player.addListener(AudioFocusListener(this, player))
        player.addListener(
            PlayerEventListener(this, scope, session, state.current, extensions, app.throwFlow)
        )
        player.addListener(
            PlayerRadio(this, scope, player, app.throwFlow, state.radio, extensions.music)
        )
        player.addListener(
            TrackingListener(player, scope, extensions.music, extensions.tracker, app.throwFlow)
        )
        player.addListener(effects)
        app.settings.registerOnSharedPreferenceChangeListener(listener)

        val notificationProvider =
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelName(R.string.app_name)
                .build()
        notificationProvider.setSmallIcon(R.drawable.ic_mono)
        setMediaNotificationProvider(notificationProvider)

        mediaSession = session
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private val intent: PendingIntent
        get() = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                putExtra("fromNotification", true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private val cache by inject<SimpleCache>()

    @OptIn(UnstableApi::class)
    private fun createExoplayer() = run {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val audioOffloadPreferences = TrackSelectionParameters.AudioOffloadPreferences.Builder()
            .setAudioOffloadMode(AUDIO_OFFLOAD_MODE_ENABLED)
            .setIsGaplessSupportRequired(true)
            .setIsSpeedChangeSupportRequired(true)
            .build()

        val factory = StreamableMediaSource.Factory(
            this, scope, app.settings, state.servers, extensionLoader.extensions.music, cache
        )

        ExoPlayer.Builder(this, factory)
            .setRenderersFactory(RenderersFactory(this))
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(audioAttributes, true)
            .build()
            .also {
                it.trackSelectionParameters = it.trackSelectionParameters
                    .buildUpon()
                    .setAudioOffloadPreferences(audioOffloadPreferences)
                    .build()
                it.preloadConfiguration = ExoPlayer.PreloadConfiguration(0)
                it.skipSilenceEnabled = app.settings.getBoolean(SKIP_SILENCE, false)
            }
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        val stopPlayer = app.settings.getBoolean(CLOSE_PLAYER, false)
        val player = mediaSession?.player ?: return stopSelf()
        if (stopPlayer || !player.isPlaying) stopSelf()
    }

    companion object {
        const val CLOSE_PLAYER = "close_player"
        const val SKIP_SILENCE = "skip_silence"

        private const val CACHE_SIZE = "cache_size"

        @OptIn(UnstableApi::class)
        fun getCache(
            app: Application,
            settings: SharedPreferences,
        ): SimpleCache {
            val databaseProvider = StandaloneDatabaseProvider(app)
            val cacheSize = settings.getInt(CACHE_SIZE, 250)
            return SimpleCache(
                File(app.cacheDir, "exoplayer"),
                LeastRecentlyUsedCacheEvictor(cacheSize * 1024 * 1024L),
                databaseProvider
            )
        }

        private const val STREAM_QUALITY = "stream_quality"
        val streamQualities = arrayOf("highest", "medium", "lowest")

        fun selectSourceIndex(
            settings: SharedPreferences?, streamables: List<Streamable>
        ) = if (streamables.isNotEmpty()) streamables.indexOf(streamables.select(settings))
        else -1

        private fun <E> List<E>.select(settings: SharedPreferences?, quality: (E) -> Int) =
            when (settings?.getString(STREAM_QUALITY, "medium")) {
                "highest" -> maxBy { quality(it) }
                "medium" -> sortedBy { quality(it) }[size / 2]
                "lowest" -> minBy { quality(it) }
                else -> first()
            }

        private fun List<Streamable>.select(settings: SharedPreferences?) =
            select(settings) { it.quality }

        fun List<Streamable.Source>.select(settings: SharedPreferences?) =
            select(settings) { it.quality }

        fun getController(
            app: Application,
            block: (MediaController) -> Unit
        ): () -> Unit {
            val sessionToken =
                SessionToken(app, ComponentName(app, PlayerService::class.java))
            val playerFuture = MediaController.Builder(app, sessionToken).buildAsync()
            app.listenFuture(playerFuture) { result ->
                val controller = result.getOrElse {
                    return@listenFuture it.printStackTrace()
                }
                block(controller)
            }
            return { MediaController.releaseFuture(playerFuture) }
        }
    }
}