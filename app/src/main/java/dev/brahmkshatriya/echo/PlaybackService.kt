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
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.playback.PlayerBitmapLoader
import dev.brahmkshatriya.echo.playback.PlayerEventListener
import dev.brahmkshatriya.echo.playback.PlayerSessionCallback
import dev.brahmkshatriya.echo.playback.Radio
import dev.brahmkshatriya.echo.playback.RenderersFactory
import dev.brahmkshatriya.echo.playback.ResumptionUtils
import dev.brahmkshatriya.echo.playback.StreamableDataSource
import dev.brahmkshatriya.echo.playback.TrackResolver
import dev.brahmkshatriya.echo.playback.TrackingListener
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.TrackerExtension
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.CLOSE_PLAYER
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.KEEP_QUEUE
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.SKIP_SILENCE
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {
    @Inject
    lateinit var extFlow: MutableStateFlow<MusicExtension?>

    @Inject
    lateinit var extListFlow: MutableStateFlow<List<MusicExtension>?>

    @Inject
    lateinit var trackerList: MutableStateFlow<List<TrackerExtension>?>

    @Inject
    lateinit var throwFlow: MutableSharedFlow<Throwable>

    @Inject
    lateinit var messageFlow: MutableSharedFlow<SnackBar.Message>

    @Inject
    lateinit var stateFlow: MutableStateFlow<Radio.State>

    @Inject
    lateinit var settings: SharedPreferences

    @Inject
    lateinit var cache: SimpleCache

    @Inject
    lateinit var current: MutableStateFlow<Current?>

    private var mediaLibrarySession: MediaLibrarySession? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        val exoPlayer = createExoplayer()

        exoPlayer.prepare()

        val intent = Intent(this, MainActivity::class.java)
            .putExtra("fromNotification", true)

        val pendingIntent = PendingIntent
            .getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val callback = PlayerSessionCallback(
            this, settings, scope, extListFlow, extFlow, throwFlow, messageFlow, stateFlow
        )

        val session = MediaLibrarySession.Builder(this, exoPlayer, callback)
            .setSessionActivity(pendingIntent)
            .setBitmapLoader(PlayerBitmapLoader(this, exoPlayer, scope))
            .build()

        val notificationProvider =
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelName(R.string.app_name)
                .build()
        notificationProvider.setSmallIcon(R.drawable.ic_mono)
        setMediaNotificationProvider(notificationProvider)

        exoPlayer.addListener(PlayerEventListener(this, session, current, extListFlow))
        exoPlayer.addListener(
            Radio(session,this, settings, scope, extListFlow, throwFlow, messageFlow, stateFlow)
        )
        exoPlayer.addListener(
            TrackingListener(session, scope, extListFlow, trackerList, throwFlow)
        )
        settings.registerOnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                SKIP_SILENCE -> exoPlayer.skipSilenceEnabled = prefs.getBoolean(key, true)
            }
        }

        val keepQueue = settings.getBoolean(KEEP_QUEUE, true)
        if (keepQueue) scope.launch {
            extListFlow.first { it != null }
            ResumptionUtils.recoverPlaylist(this@PlaybackService).apply {
                exoPlayer.setMediaItems(mediaItems, startIndex, startPositionMs)
                exoPlayer.prepare()
            }
        }

        this.mediaLibrarySession = session
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

        val trackResolver = TrackResolver(this, extListFlow, settings)

        val dataSourceFactory = ResolvingDataSource.Factory(cacheFactory, trackResolver)
        val factory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(this, factory)
            .setRenderersFactory(RenderersFactory(this))
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSkipSilenceEnabled(settings.getBoolean(SKIP_SILENCE, true))
            .setAudioAttributes(audioAttributes, true)
            .build()
            .also { trackResolver.player = it }
    }

    override fun onDestroy() {
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