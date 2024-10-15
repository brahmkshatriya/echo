package dev.brahmkshatriya.echo

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.playback.PlayerCallback
import dev.brahmkshatriya.echo.playback.ResumptionUtils
import dev.brahmkshatriya.echo.playback.listeners.AudioFocusListener
import dev.brahmkshatriya.echo.playback.listeners.PlayerEventListener
import dev.brahmkshatriya.echo.playback.listeners.Radio
import dev.brahmkshatriya.echo.playback.listeners.TrackingListener
import dev.brahmkshatriya.echo.playback.render.FFTAudioProcessor
import dev.brahmkshatriya.echo.playback.render.PlayerBitmapLoader
import dev.brahmkshatriya.echo.playback.render.RenderersFactory
import dev.brahmkshatriya.echo.playback.source.MediaFactory
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
    lateinit var fftAudioProcessor: FFTAudioProcessor

    private val scope = CoroutineScope(Dispatchers.Main)

    @OptIn(UnstableApi::class)
    private fun createExoplayer(extListFlow: MutableStateFlow<List<MusicExtension>?>) = run {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val factory = MediaFactory(
            cache, this, scope, extListFlow, settings, throwFlow
        )

        ExoPlayer.Builder(this, factory)
            .setRenderersFactory(RenderersFactory(this, fftAudioProcessor))
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSkipSilenceEnabled(settings.getBoolean(SKIP_SILENCE, true))
            .setAudioAttributes(audioAttributes, true)
            .build()
            .also { factory.setPlayer(it) }
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

        val intent = Intent(this, MainActivity::class.java)
            .putExtra("fromNotification", true)

        val pendingIntent = PendingIntent
            .getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val callback = PlayerCallback(
            this, settings, scope, extListFlow, extFlow, throwFlow, messageFlow, stateFlow
        )

        val session = MediaLibrarySession.Builder(this, exoPlayer, callback)
            .setSessionActivity(pendingIntent)
            .setBitmapLoader(PlayerBitmapLoader(this, scope))
            .build()

        val notificationProvider =
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelName(R.string.app_name)
                .build()
        notificationProvider.setSmallIcon(R.drawable.ic_mono)
        setMediaNotificationProvider(notificationProvider)

        exoPlayer.addListener(PlayerEventListener(this, session, current, extListFlow))
        exoPlayer.addListener(AudioFocusListener(this, exoPlayer))
        exoPlayer.addListener(
            Radio(exoPlayer, this, settings, scope, extListFlow, throwFlow, messageFlow, stateFlow)
        )
        exoPlayer.addListener(
            TrackingListener(exoPlayer, scope, extListFlow, trackerList, throwFlow)
        )
        settings.registerOnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                SKIP_SILENCE -> exoPlayer.skipSilenceEnabled = prefs.getBoolean(key, true)
            }
        }

        //TODO: extension updater
        //TODO: Spotify
        //TODO: EQ, Pitch, Tempo, Reverb & Sleep Timer(5m, 10m, 15m, 30m, 45m, 1hr, End of track)
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