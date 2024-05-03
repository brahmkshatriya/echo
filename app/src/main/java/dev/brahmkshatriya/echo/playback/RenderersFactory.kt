package dev.brahmkshatriya.echo.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor

@OptIn(UnstableApi::class)
class RenderersFactory(context: Context) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ) = run {

        val minDuration = 10_00_000L
        val ratio = 100f
        val minVolume = 0
        val threshHold = 256.toShort()
        val silenceSkippingAudioProcessor =
            SilenceSkippingAudioProcessor(minDuration, ratio, minDuration, minVolume, threshHold)

        DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessorChain(
                DefaultAudioSink.DefaultAudioProcessorChain(
                    emptyArray(), silenceSkippingAudioProcessor, SonicAudioProcessor()
                )
            )
            .build()
    }
}