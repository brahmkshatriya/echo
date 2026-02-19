package dev.brahmkshatriya.echo.playback.listener

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import androidx.media3.common.Player
import androidx.media3.common.Player.PlaybackSuppressionReason

@Suppress("DEPRECATION")
class AudioFocusListener(
    val context: Context,
    val player: Player
) : Player.Listener {
    private val handler = Handler(context.mainLooper)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var resumeOnFocusGain = false
    private var waitingForFocus = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                player.volume = 1f
                if (resumeOnFocusGain || waitingForFocus) {
                    resumeOnFocusGain = false
                    waitingForFocus = false
                    player.play()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                waitingForFocus = false
                player.pause()
                abandonRequest()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                resumeOnFocusGain = player.playWhenReady
                waitingForFocus = true
                player.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                player.volume = 0.2f
            }
        }
    }


    private fun requestFocus() {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(focusRequest)
        } else {
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        
        when (result) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                player.volume = 1f
                waitingForFocus = false
            }
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                waitingForFocus = true
                resumeOnFocusGain = true
                player.pause()
            }
            else -> {
                waitingForFocus = false
                player.pause()
            }
        }
    }

    private fun abandonRequest() {
        waitingForFocus = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            audioManager.abandonAudioFocusRequest(focusRequest)
        else audioManager.abandonAudioFocus(audioFocusChangeListener)
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setAcceptsDelayedFocusGain(true)
                setOnAudioFocusChangeListener(audioFocusChangeListener, handler)
                build()
            }
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (playWhenReady) {
            requestFocus()
        } else {
            if (!waitingForFocus) {
                abandonRequest()
            }
        }
    }

    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: @PlaybackSuppressionReason Int) {
        if (playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS) {
            requestFocus()
        }
    }
}