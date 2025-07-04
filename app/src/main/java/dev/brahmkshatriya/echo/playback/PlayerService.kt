package dev.brahmkshatriya.echo.playback

import android.app.Application
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
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
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.extensionPrefId
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.prefs
import dev.brahmkshatriya.echo.playback.listener.AudioFocusListener
import dev.brahmkshatriya.echo.playback.listener.EffectsListener
import dev.brahmkshatriya.echo.playback.listener.MediaSessionServiceListener
import dev.brahmkshatriya.echo.playback.listener.PlayerEventListener
import dev.brahmkshatriya.echo.playback.listener.PlayerRadio
import dev.brahmkshatriya.echo.playback.listener.TrackingListener
import dev.brahmkshatriya.echo.playback.renderer.PlayerBitmapLoader
import dev.brahmkshatriya.echo.playback.renderer.RenderersFactory
import dev.brahmkshatriya.echo.playback.source.StreamableMediaSource
import dev.brahmkshatriya.echo.utils.ContextUtils.getSettings
import dev.brahmkshatriya.echo.utils.ContextUtils.listenFuture
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.File

class PlayerService : MediaLibraryService() {

    private val extensionLoader by inject<ExtensionLoader>()
    private val extensions by lazy { extensionLoader }
    private val exoPlayer by lazy { createExoplayer() }

    private var mediaSession: MediaLibrarySession? = null
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    private val app by inject<App>()
    private val state by inject<PlayerState>()
    private val scope = CoroutineScope(Dispatchers.IO) + CoroutineName("PlayerService")

    @OptIn(UnstableApi::class)
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            SKIP_SILENCE -> exoPlayer.skipSilenceEnabled = prefs.getBoolean(key, true)
            MORE_BRAIN_CAPACITY -> exoPlayer.trackSelectionParameters =
                exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setAudioOffloadPreferences(offloadPreferences(prefs.getBoolean(key, false)))
                    .build()
        }
    }
    private val effects by lazy { EffectsListener(exoPlayer, this, state.session) }

    private val downloader by inject<Downloader>()
    private val downloadFlow by lazy { downloader.flow }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        setListener(MediaSessionServiceListener(this, getPendingIntent(this)))

        val player = ShufflePlayer(exoPlayer)
        scope.launch(Dispatchers.Main) {
            mediaChangeFlow.collect { (o, n) -> player.onMediaItemChanged(o, n) }
        }

        val callback = PlayerCallback(
            this, scope, app.throwFlow, extensions, state.radio, downloadFlow
        )

        val session = MediaLibrarySession.Builder(this, player, callback)
            .setBitmapLoader(PlayerBitmapLoader(this, scope))
            .setSessionActivity(getPendingIntent(this))
            .build()

        player.addListener(AudioFocusListener(this, player))
        player.addListener(
            PlayerEventListener(this, scope, session, state.current, extensions, app.throwFlow)
        )
        player.addListener(
            PlayerRadio(
                this, scope, player, app.throwFlow, state.radio, extensions.music, downloadFlow
            )
        )
        player.addListener(
            TrackingListener(player, scope, extensions, state.current, app.throwFlow)
        )
        player.addListener(effects)
        app.settings.registerOnSharedPreferenceChangeListener(listener)

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
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

    private val cache by inject<SimpleCache>()

    private val mediaChangeFlow = MutableSharedFlow<Pair<MediaItem, MediaItem>>()

    @OptIn(UnstableApi::class)
    private fun offloadPreferences(moreBrainCapacity: Boolean) =
        TrackSelectionParameters.AudioOffloadPreferences.Builder()
            .setAudioOffloadMode(
                if (moreBrainCapacity) AUDIO_OFFLOAD_MODE_DISABLED else AUDIO_OFFLOAD_MODE_ENABLED
            ).setIsGaplessSupportRequired(true)
            .setIsSpeedChangeSupportRequired(true)
            .build()

    @OptIn(UnstableApi::class)
    private fun createExoplayer() = run {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val audioOffloadPreferences =
            offloadPreferences(app.settings.getBoolean(MORE_BRAIN_CAPACITY, false))

        val factory = StreamableMediaSource.Factory(
            this, scope, state, extensions, cache, downloadFlow, mediaChangeFlow
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
                it.preloadConfiguration = ExoPlayer.PreloadConfiguration(C.TIME_UNSET)
                it.skipSilenceEnabled = app.settings.getBoolean(SKIP_SILENCE, true)
            }
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        val stopPlayer = app.settings.getBoolean(CLOSE_PLAYER, false)
        val player = mediaSession?.player ?: return stopSelf()
        if (stopPlayer || !player.isPlaying) stopSelf()
    }

    companion object {
        const val MORE_BRAIN_CAPACITY = "offload"
        const val CLOSE_PLAYER = "close_player"
        const val SKIP_SILENCE = "skip_silence"

        const val CACHE_SIZE = "cache_size"

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

        const val STREAM_QUALITY = "stream_quality"
        const val UNMETERED_STREAM_QUALITY = "unmetered_stream_quality"
        val streamQualities = arrayOf("highest", "medium", "lowest")

        suspend fun selectServerIndex(
            context: Context,
            extensionId: String,
            streamables: List<Streamable>,
            downloaded: List<String>
        ) = if (downloaded.isNotEmpty()) streamables.size
        else if (streamables.isNotEmpty()) {
            val streamable = streamables.select(context, extensionId) { it.quality }
            streamables.indexOf(streamable)
        } else -1

        private suspend fun isUnmetered(context: Context) = withContext(Dispatchers.IO) {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return@withContext false
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(network) ?: return@withContext false
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        }

        private suspend fun <E> List<E>.select(
            context: Context,
            settings: SharedPreferences,
            quality: (E) -> Int,
            default: String = streamQualities[1]
        ): E? {
            val unmetered = if (isUnmetered(context)) selectQuality(
                settings.getString(UNMETERED_STREAM_QUALITY, "off"),
                quality
            ) else null
            return unmetered ?: selectQuality(
                settings.getString(STREAM_QUALITY, default),
                quality
            )
        }

        private fun <E> List<E>.selectQuality(final: String?, quality: (E) -> Int): E? {
            return when (final) {
                streamQualities[0] -> maxBy { quality(it) }
                streamQualities[1] -> sortedBy { quality(it) }[size / 2]
                streamQualities[2] -> minBy { quality(it) }
                else -> null
            }
        }


        suspend fun <T> List<T>.select(
            context: Context, extensionId: String, quality: (T) -> Int
        ): T {
            val extSettings = extensionPrefId(ExtensionType.MUSIC.name, extensionId).prefs(context)
            return select(context, extSettings, quality, "off")
                ?: select(context, context.getSettings(), quality)
                ?: first()
        }

        fun getController(
            context: Application,
            block: (MediaController) -> Unit
        ): () -> Unit {
            val sessionToken =
                SessionToken(context, ComponentName(context, PlayerService::class.java))
            val playerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            context.listenFuture(playerFuture) { result ->
                val controller = result.getOrElse {
                    return@listenFuture it.printStackTrace()
                }
                block(controller)
            }
            return { MediaController.releaseFuture(playerFuture) }
        }

        fun getPendingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                putExtra("fromNotification", true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}