package dev.brahmkshatriya.echo.app.playback

import kotlin.math.abs
import kotlin.math.min

/** A display-frame clock gently reconciled with the backend's coarse position samples. */
internal class PlayerPositionClock {
    private var initialized = false
    private var samplePositionMs = 0L
    private var sampleTimeNanos = 0L
    private var previousFrameNanos = 0L
    private var positionMs = 0.0
    private var wasPlaying = false
    private var previousSpeed = 1.0

    fun reset() {
        initialized = false
    }

    fun positionAt(
        reportedMs: Long,
        frameTimeNanos: Long,
        playing: Boolean,
        speed: Double,
    ): Double {
        val playbackSpeed = speed.takeIf { it.isFinite() && it >= 0.0 } ?: 1.0
        if (!initialized) {
            initialized = true
            samplePositionMs = reportedMs
            sampleTimeNanos = frameTimeNanos
            previousFrameNanos = frameTimeNanos
            positionMs = reportedMs.coerceAtLeast(0L).toDouble()
            wasPlaying = playing
            previousSpeed = playbackSpeed
            return positionMs
        }

        val wasAdvancing = wasPlaying
        val changed = reportedMs != samplePositionMs
        if (changed || playing != wasPlaying || playbackSpeed != previousSpeed) {
            samplePositionMs = reportedMs
            sampleTimeNanos = frameTimeNanos
        }
        wasPlaying = playing
        previousSpeed = playbackSpeed

        // Cover Android's 250 ms reports and scheduling jitter, but stop if the
        // backend stops advancing without publishing a buffering/paused state.
        val deadlineNanos = sampleTimeNanos + 750_000_000L
        val advancingNanos = (min(frameTimeNanos, deadlineNanos) -
                min(previousFrameNanos, deadlineNanos)).coerceAtLeast(0L)
        previousFrameNanos = frameTimeNanos
        // The first resumed frame must not include time spent paused while the
        // display was asleep or this view had no frames.
        val advanceMs = if (playing && wasAdvancing) {
            advancingNanos / 1_000_000.0 * playbackSpeed
        } else 0.0
        val predictedMs = positionMs + advanceMs

        // A seek or a resumed backend after a long stall needs a new origin.
        // Small sample jitter is corrected gradually below, without rewinding.
        if (changed && abs(reportedMs - predictedMs) > 1_000.0) {
            positionMs = reportedMs.coerceAtLeast(0L).toDouble()
        } else if (!playing) {
            positionMs = maxOf(positionMs, reportedMs.toDouble())
        } else if (advanceMs > 0.0) {
            val sampleAgeMs = (frameTimeNanos - sampleTimeNanos)
                .coerceIn(0L, 750_000_000L) / 1_000_000.0
            val targetMs = reportedMs + sampleAgeMs * playbackSpeed
            val frameMs = advancingNanos / 1_000_000.0
            val correction = ((targetMs - predictedMs) * (1.0 - kotlin.math.exp(-frameMs / 150.0)))
                .coerceIn(-advanceMs * 0.25, advanceMs * 0.25)
            positionMs = predictedMs + correction
        }
        return positionMs
    }
}
