package dev.brahmkshatriya.echo

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
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
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.playback.PlayerCallback
import dev.brahmkshatriya.echo.playback.ResumptionUtils
import dev.brahmkshatriya.echo.playback.listeners.AudioFocusListener
import dev.brahmkshatriya.echo.playback.listeners.EffectsListener
import dev.brahmkshatriya.echo.playback.listeners.EffectsListener.Companion.SKIP_SILENCE
import dev.brahmkshatriya.echo.playback.listeners.PlayerEventListener
import dev.brahmkshatriya.echo.playback.listeners.Radio
import dev.brahmkshatriya.echo.playback.listeners.TrackingListener
import dev.brahmkshatriya.echo.playback.loading.StreamableMediaSource
import dev.brahmkshatriya.echo.playback.render.PlayerBitmapLoader
import dev.brahmkshatriya.echo.playback.render.RenderersFactory
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.CLOSE_PLAYER
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.plus
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlayerService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    @Inject
    lateinit var extensionLoader: ExtensionLoader

    @Inject
    lateinit var throwFlow: MutableSharedFlow<Throwable>

    @Inject
    lateinit var messageFlow: MutableSharedFlow<Message>

    @Inject
    lateinit var stateFlow: MutableStateFlow<Radio.State>

    @Inject
    lateinit var audioSessionFlow: MutableStateFlow<Int>

    @Inject
    lateinit var settings: SharedPreferences

    @Inject
    @SuppressLint("UnsafeOptInUsageError")
    lateinit var cache: SimpleCache

    @Inject
    lateinit var current: MutableStateFlow<Current?>

    @Inject
    lateinit var currentServers: MutableStateFlow<Map<String, Streamable.Media.Server>>

    private val scope = CoroutineScope(Dispatchers.IO) + CoroutineName("PlayerService")

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
            .setAudioAttributes(audioAttributes, true)
            .build()
            .also {
                it.trackSelectionParameters = it.trackSelectionParameters
                    .buildUpon()
                    .setAudioOffloadPreferences(audioOffloadPreferences)
                    .build()
                it.preloadConfiguration = ExoPlayer.PreloadConfiguration(0)
                it.skipSilenceEnabled = settings.getBoolean(SKIP_SILENCE, false)
            }
    }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            SKIP_SILENCE -> exoPlayer.skipSilenceEnabled = prefs.getBoolean(key, false)
        }
    }

    val intent: PendingIntent
        get() = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                putExtra("fromNotification", true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private val exoPlayer by lazy { createExoplayer(extensionLoader.extensions) }
    private val effects by lazy { EffectsListener(exoPlayer, this, audioSessionFlow) }
    override fun onCreate() {
        super.onCreate()
        setListener(MediaSessionServiceListener())

        val extListFlow = extensionLoader.extensions
        val extFlow = extensionLoader.current
        val trackerList = extensionLoader.trackers

        exoPlayer.prepare()

//        val player = ShufflePlayer(exoPlayer)
        val player = exoPlayer

        val callback = PlayerCallback(
            this, settings, scope, extListFlow, extFlow, throwFlow, messageFlow, stateFlow
        )

        val session = MediaLibrarySession.Builder(this, player, callback)
            .setSessionActivity(intent)
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
        player.addListener(effects)
        settings.registerOnSharedPreferenceChangeListener(listener)
        mediaSession = session
    }

    override fun onDestroy() {
        mediaSession?.run {
            ResumptionUtils.saveQueue(this@PlayerService, player)
            effects.release()
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val stopPlayer = settings.getBoolean(CLOSE_PLAYER, false)
        val player = mediaSession?.player ?: return stopSelf()
        if (stopPlayer || !player.isPlaying) stopSelf()
    }

    private inner class MediaSessionServiceListener : Listener {
        val analytics = FirebaseAnalytics.getInstance(this@PlayerService)
        override fun onForegroundServiceStartNotAllowedException() {

            if (
                Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) return
            analytics.logEvent("foreground_service_not_allowed") {
                bundleOf(
                    "type" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) this@PlayerService.foregroundServiceType else null,
                    "player" to exoPlayer.mediaItemCount,
                    "wasPlaying" to exoPlayer.isPlaying
                )
            }
            val notificationManagerCompat = NotificationManagerCompat.from(this@PlayerService)
            ensureNotificationChannel(notificationManagerCompat)
            val builder = NotificationCompat.Builder(this@PlayerService, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mono)
                .setContentTitle(getString(R.string.app_name))
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(getString(R.string.app_name))
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .also { it.setContentIntent(intent) }
            notificationManagerCompat.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun ensureNotificationChannel(notificationManagerCompat: NotificationManagerCompat) {
        if (
            Build.VERSION.SDK_INT < 26 ||
            notificationManagerCompat.getNotificationChannel(CHANNEL_ID) != null
        ) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManagerCompat.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_ID = 123
        private const val CHANNEL_ID = "echo_notification_channel_id"
    }
}