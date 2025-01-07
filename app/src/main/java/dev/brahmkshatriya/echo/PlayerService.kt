package dev.brahmkshatriya.echo

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.playback.PlayerCallback
import dev.brahmkshatriya.echo.playback.ResumptionUtils
import dev.brahmkshatriya.echo.playback.listeners.AudioFocusListener
import dev.brahmkshatriya.echo.playback.listeners.PlayerEventListener
import dev.brahmkshatriya.echo.playback.listeners.Radio
import dev.brahmkshatriya.echo.playback.listeners.TrackingListener
import dev.brahmkshatriya.echo.playback.loading.StreamableMediaSource
import dev.brahmkshatriya.echo.playback.render.PlayerBitmapLoader
import dev.brahmkshatriya.echo.playback.render.RenderersFactory
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.CLOSE_PLAYER
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.SKIP_SILENCE
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class PlayerService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    @Inject
    lateinit var extensionLoader: ExtensionLoader

    @Inject
    lateinit var throwFlow: MutableSharedFlow<Throwable>

    @Inject
    lateinit var messageFlow: MutableSharedFlow<SnackBar.Message>

    @Inject
    lateinit var stateFlow: MutableStateFlow<Radio.State>

    @Inject
    lateinit var settings: SharedPreferences

    @Inject
    @SuppressLint("UnsafeOptInUsageError")
    lateinit var cache: SimpleCache

    @Inject
    lateinit var current: MutableStateFlow<Current?>

    @Inject
    lateinit var currentServers: MutableStateFlow<Map<String, Streamable.Media.Server>>

    private val scope = CoroutineScope(Dispatchers.IO)

    @OptIn(UnstableApi::class)
    private fun createExoplayer(extListFlow: MutableStateFlow<List<MusicExtension>?>) = run {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val audioOffloadPreferences = TrackSelectionParameters.AudioOffloadPreferences.Builder()
            .setAudioOffloadMode(AUDIO_OFFLOAD_MODE_ENABLED)
            // Add additional options as needed
            .setIsGaplessSupportRequired(true)
            .setIsSpeedChangeSupportRequired(true)
            .build()

        val factory = StreamableMediaSource.Factory(
            this, scope, settings, currentServers, extListFlow, cache
        )

        ExoPlayer.Builder(this, factory)
            .setRenderersFactory(RenderersFactory(this))
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSkipSilenceEnabled(settings.getBoolean(SKIP_SILENCE, true))
            .setAudioAttributes(audioAttributes, true)
            .build()
            .also {
                it.trackSelectionParameters = it.trackSelectionParameters
                    .buildUpon()
                    .setAudioOffloadPreferences(audioOffloadPreferences)
                    .build()

                it.preloadConfiguration = ExoPlayer.PreloadConfiguration(0)
            }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        extensionLoader.initialize()

        val extListFlow = extensionLoader.extensions
        val extFlow = extensionLoader.current
        val trackerList = extensionLoader.trackers

        val exoPlayer = createExoplayer(extListFlow)
        exoPlayer.prepare()

//        val player = ShufflePlayer(exoPlayer)
        val player = exoPlayer

        val intent = Intent(this, MainActivity::class.java)
            .putExtra("fromNotification", true)

        val pendingIntent = PendingIntent
            .getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val callback = PlayerCallback(
            this, settings, scope, extListFlow, extFlow, throwFlow, messageFlow, stateFlow
        )

        val session = MediaLibrarySession.Builder(this, player, callback)
            .setSessionActivity(pendingIntent)
            .setBitmapLoader(PlayerBitmapLoader(this, scope))
            .build()

        val notificationProvider =
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelName(R.string.app_name)
                .build()
        notificationProvider.setSmallIcon(R.drawable.ic_mono)
        setMediaNotificationProvider(notificationProvider)

        player.addListener(AudioFocusListener(this, player))
        player.addListener(
            PlayerEventListener(this, scope, session, current, extListFlow, throwFlow)
        )
        player.addListener(
            Radio(player, this, settings, scope, extListFlow, throwFlow, messageFlow, stateFlow)
        )
        player.addListener(
            TrackingListener(player, scope, extListFlow, trackerList, throwFlow)
        )
        settings.registerOnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                SKIP_SILENCE -> exoPlayer.skipSilenceEnabled = prefs.getBoolean(key, true)
            }
        }

//        val equalizer = Equalizer(1, exoPlayer.audioSessionId)

        mediaSession = session
    }

    override fun onDestroy() {
        mediaSession?.run {
            ResumptionUtils.saveQueue(this@PlayerService, player)
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val stopPlayer = settings.getBoolean(CLOSE_PLAYER, true)
        val player = mediaSession?.player ?: return stopSelf()
        if (stopPlayer || !player.isPlaying) stopSelf()
    }
}