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
    private lateinit var focusRequest: AudioFocusRequest

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener {
        if (it == AudioManager.AUDIOFOCUS_GAIN) {
            player.apply {
                setAudioAttributes(audioAttributes, true)
                seekTo(currentPosition)
            }
            abandonRequest()
        }
    }


    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            audioManager.requestAudioFocus(focusRequest)
        else audioManager.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
    }

    private fun abandonRequest() {
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

        //TODO fix this to support player to play in calls
        // if the audio suppression was not there from the start
        // https://github.com/androidx/media/issues/1716
        onPlaybackSuppressionReasonChanged(player.playbackSuppressionReason)
    }

    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: @PlaybackSuppressionReason Int) {
        if (playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS) {
            player.apply {
                setAudioAttributes(audioAttributes, false)
                player.playWhenReady = false
                seekTo(currentPosition)
            }
            requestFocus()
        }
    }
}