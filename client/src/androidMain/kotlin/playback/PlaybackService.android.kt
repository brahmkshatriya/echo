package dev.brahmkshatriya.echo.app.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import dev.brahmkshatriya.kmedianotification.MediaNotificationService
import dev.brahmkshatriya.kmedianotification.MediaSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val PlaybackWakeLockTimeoutMs = 10 * 60 * 1_000L

/** Echo owns audio policy; KMediaNotification hosts the system session and foreground notification. */
class EchoPlaybackService : MediaNotificationService() {
    private val playback by lazy { (application as PlaybackComponentOwner).playbackComponent }
    private val session get() = playback.session
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var audio: AudioManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private var focusHeld = false
    private var focusRequest: AudioFocusRequest? = null
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (!ownsMediaSession) return@OnAudioFocusChangeListener
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                focusHeld = false
                session.engine.setDucking(1f)
                session.pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> session.engine.setDucking(0.2f)
            AudioManager.AUDIOFOCUS_GAIN -> {
                focusHeld = true; session.engine.setDucking(1f)
            }
        }
    }
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) session.pause()
        }
    }

    override fun provideMediaSession(): MediaSession = playback.notification
    override fun onCreate() {
        audio = getSystemService(AudioManager::class.java)
        wakeLock = getSystemService(PowerManager::class.java).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "$packageName:playback"
        ).apply { setReferenceCounted(false) }
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(
            noisyReceiver,
            filter,
            RECEIVER_NOT_EXPORTED
        )
        else registerReceiver(noisyReceiver, filter)
        super.onCreate()
        scope.launch {
            provideMediaSession().state
                .map { it.playback.playWhenReady }
                .distinctUntilChanged()
                .collectLatest { playing ->
                    if (!playing) {
                        if (focusHeld) abandonFocus()
                        return@collectLatest
                    }
                    if (!focusHeld && !requestFocus()) {
                        session.pause()
                        session.reportError(
                            "Playback unavailable",
                            IllegalStateException("Audio focus is currently unavailable")
                        )
                        return@collectLatest
                    }
                    try {
                        while (this@launch.isActive) {
                            // Renew before expiry so long playback has no gap in protection.
                            wakeLock.acquire(PlaybackWakeLockTimeoutMs)
                            delay((PlaybackWakeLockTimeoutMs / 2).milliseconds)
                        }
                    } finally {
                        if (wakeLock.isHeld) wakeLock.release()
                    }
                }
        }
    }

    @Suppress("DEPRECATION")
    private fun requestFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= 26) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(focusListener).build()
            focusRequest = request
            audio.requestAudioFocus(request)
        } else audio.requestAudioFocus(
            focusListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        focusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (focusHeld) session.engine.setDucking(1f)
        return focusHeld
    }

    @Suppress("DEPRECATION")
    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= 26) focusRequest?.let(audio::abandonAudioFocusRequest)
        else audio.abandonAudioFocus(focusListener)
        focusHeld = false
        session.engine.setDucking(1f)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!session.isPlaying) {
            session.clearQueue(); stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        if (ownsMediaSession && session.isPlaying) session.pause()
        if (wakeLock.isHeld) wakeLock.release()
        abandonFocus()
        unregisterReceiver(noisyReceiver)
        super.onDestroy()
    }
}
