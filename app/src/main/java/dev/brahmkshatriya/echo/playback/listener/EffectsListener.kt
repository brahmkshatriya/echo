package dev.brahmkshatriya.echo.playback.listener

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.copyTo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
class EffectsListener(
    private val exoPlayer: ExoPlayer,
    private val context: Context,
    private val audioSessionFlow: MutableStateFlow<Int>
) : Player.Listener {

    init {
        audioSessionFlow.value = exoPlayer.audioSessionId
        context.broadcastAudioSession()
    }

    private val settings: SharedPreferences = context.globalFx()
    private var oldSettings = settings
    private fun applyCustomEffects() {
        oldSettings.unregisterOnSharedPreferenceChangeListener(listener)
        val current = context.getFxPrefs(settings, exoPlayer.currentMediaItem?.mediaId?.hashCode())
            ?: settings
        oldSettings = current
        current.registerOnSharedPreferenceChangeListener(listener)
        applyPlayback(current)
        effects.applySettings(current)
    }

    private fun createEffects() = Effects(exoPlayer.audioSessionId)

    private fun applyPlayback(settings: SharedPreferences) {
        val index = settings.getInt(PLAYBACK_SPEED, speedRange.indexOf(1f))
        val speed = speedRange.getOrNull(index) ?: 1f
        val pitch = if (settings.getBoolean(CHANGE_PITCH, true)) speed else 1f
        exoPlayer.playbackParameters =
            PlaybackParameters(speed, pitch)
    }

    private var effects: Effects = createEffects()
    private val listener = OnSharedPreferenceChangeListener { _, _ -> applyCustomEffects() }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = applyCustomEffects()
    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        release()
        context.broadcastAudioSession()
        audioSessionFlow.value = audioSessionId
        effects = createEffects()
        effects.applySettings(oldSettings)
    }

    class Effects(sessionId: Int) {
        private val equalizer = runCatching { Equalizer(0, sessionId) }.getOrNull()
        private val gain = runCatching { LoudnessEnhancer(sessionId) }.getOrNull()
        private fun applyBassBoost(strength: Int) = runCatching {
            if (strength == 0) {
                equalizer?.setEnabled(false)
                gain?.setEnabled(false)
                return@runCatching
            }
            gain?.setEnabled(true)
            equalizer?.setEnabled(true)
            equalizer?.apply {
                val value =
                    (strength * bandLevelRange.last().toDouble() / 10).roundToInt().toShort()
                val zero = numberOfBands.toDouble() * 2 / 3
                for (it in 0 until numberOfBands) {
                    val v = (-(it - zero).pow(3) * value / zero.pow(3)).roundToInt()
                    setBandLevel(it.toShort(), v.toShort())
                }
            }
            val g = (strength.toDouble().pow(1.toDouble() / 3) * 1600).roundToInt()
            gain?.setTargetGain(g)
        }

        fun release() {
            runCatching {
                equalizer?.release()
                gain?.release()
            }
        }

        fun applySettings(settings: SharedPreferences) {
            applyBassBoost(settings.getInt(BASS_BOOST, 0))
        }
    }

    companion object {
        private const val GLOBAL_FX = "global_fx"
        const val BASS_BOOST = "bass_boost"
        const val PLAYBACK_SPEED = "playback_speed"
        val speedRange = listOf(
            0.1f, 0.175f, 0.25f, 0.33f, 0.5f, 0.66f, 0.75f, 0.85f, 0.9f, 0.95f,
            1f, 1.05f, 1.1f, 1.15f, 1.25f, 1.33f, 1.5f, 1.66f, 1.75f, 1.88f, 2f,
            2.33f, 2.5f, 3f, 4f, 8f, 16f, 32f, 64f
        )

        const val CHANGE_PITCH = "change_pitch"
        const val CUSTOM_EFFECTS = "custom_effects"

        fun Context.globalFx() = getSharedPreferences(GLOBAL_FX, Context.MODE_PRIVATE)!!
        fun Context.deleteGlobalFx() = deleteSharedPreferences(GLOBAL_FX)
        fun Context.getFxPrefs(settings: SharedPreferences, id: Int? = null): SharedPreferences? {
            if (id == null) return null
            val string = id.toString()
            val hasCustom = settings.getStringSet(CUSTOM_EFFECTS, emptySet())?.contains(string)
                ?: false
            return if (!hasCustom) null
            else getSharedPreferences("fx_$string", Context.MODE_PRIVATE)!!.apply {
                if (getBoolean("init", false)) return@apply
                settings.copyTo(this)
                edit { putBoolean("init", true) }
            }
        }

        fun Context.deleteFxPrefs(id: Int) =
            deleteSharedPreferences("fx_$id")
    }

    private fun Context.broadcastAudioSession() {
        val id = exoPlayer.audioSessionId
        sendBroadcast(Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, id)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        })
    }

    private fun Context.broadcastAudioSessionClose(id: Int) {
        sendBroadcast(Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, id)
        })
    }

    private fun release() {
        effects.release()
        context.broadcastAudioSessionClose(audioSessionFlow.value)
    }
}