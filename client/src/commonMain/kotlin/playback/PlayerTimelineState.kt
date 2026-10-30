package dev.brahmkshatriya.echo.app.playback

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class PlayerTimelineState(
    durationMs: Float,
    private val timeSource: kotlin.time.TimeSource = kotlin.time.TimeSource.Monotonic,
) {
    var durationMs by mutableFloatStateOf(durationMs.coerceAtLeast(0f))
        private set
    var reportedPositionMs by mutableFloatStateOf(0f)
        private set
    var playbackSpeed by mutableStateOf(1.0)
        private set
    var backendPlaying by mutableStateOf(false)
        private set
    private val displayClock = PlayerPositionClock()
    private var firstSample = true
    var positionMs by mutableFloatStateOf(0f)
        private set
    var bufferedMs by mutableStateOf<Float?>(null)
        private set
    var isSeeking by mutableStateOf(false)
        private set
    /** Freeze the displayed thumb at a newly requested seek until it is acknowledged. */
    private var pendingSeek: Pair<Float, kotlin.time.TimeMark>? = null
    private var acceptNextPlayerPosition = true
    /** A seek made before the current media is ready to accept it. */
    var deferredSeekMs by mutableStateOf<Float?>(null)
        private set

    val isAwaitingSeek: Boolean
        get() = isSeeking || deferredSeekMs != null || pendingSeek != null

    fun previewSeek(position: Float) {
        isSeeking = true
        positionMs = clamp(position)
    }

    fun finishScrub() { isSeeking = false }

    fun cancelScrub() {
        if (!isSeeking) return
        isSeeking = false
        displayClock.reset()
        positionMs = clamp(reportedPositionMs)
    }

    fun markComplete() {
        if (durationMs > 0f) positionMs = durationMs
    }

    fun resetPosition(targetMs: Float = 0f) {
        isSeeking = false
        displayClock.reset()
        cancelPendingSeek()
        positionMs = clamp(targetMs)
    }

    fun beginSeek(targetMs: Float) {
        displayClock.reset()
        positionMs = clamp(targetMs)
        deferredSeekMs = null
        acceptNextPlayerPosition = false
        pendingSeek = positionMs to timeSource.markNow()
    }

    fun deferSeek(targetMs: Float) {
        positionMs = clamp(targetMs)
        pendingSeek = null
        acceptNextPlayerPosition = false
        deferredSeekMs = positionMs
    }

    fun cancelPendingSeek() {
        pendingSeek = null
        deferredSeekMs = null
        acceptNextPlayerPosition = true
    }

    fun applyPlayerPosition(reportedMs: Float, discontinuity: Boolean = false) {
        // The media can take arbitrarily long to open; don't time out a seek
        // before it has even been submitted to the backend.
        if (isSeeking || deferredSeekMs != null) return
        var resetPosition = discontinuity || acceptNextPlayerPosition
        pendingSeek?.let { (target, started) ->
            val elapsed = started.elapsedNow()
            val reached = kotlin.math.abs(target - reportedMs) <= 250f
            // The backend may first echo the requested position, then briefly
            // report an old one while its decoder finishes seeking. Suppress
            // both for the first few playback frames.
            if (elapsed < kotlin.time.Duration.parse("350ms") ||
                (!reached && elapsed < kotlin.time.Duration.parse("3s"))) return
            pendingSeek = null
            // A rejected/timed-out seek must be able to restore the real position.
            resetPosition = !reached
        }
        acceptNextPlayerPosition = false
        val nextPositionMs = clamp(reportedMs)
        // New samples can trail the frame-interpolated position. Hold until
        // playback catches up instead of pulling the lyrics and seekbar back.
        positionMs = if (resetPosition) nextPositionMs else maxOf(positionMs, nextPositionMs)
    }

    private fun clamp(value: Float): Float = value.coerceIn(0f,
        if (durationMs > 0f) durationMs else Float.MAX_VALUE)

    fun beginSelection() {
        firstSample = true
        displayClock.reset()
    }

    fun updateBackend(state: dev.brahmkshatriya.kmplayer.KMPlayerState) {
        state.duration?.inWholeMilliseconds?.takeIf { it > 0L }?.let { durationMs = it.toFloat() }
        bufferedMs = state.bufferedPosition?.inWholeMilliseconds?.toFloat()
        playbackSpeed = state.speed
        backendPlaying = state.isPlaying
        reportedPositionMs = state.position.inWholeMilliseconds.coerceAtLeast(0L).toFloat()
        applyPlayerPosition(reportedPositionMs, discontinuity = firstSample)
        firstSample = false
    }

    /** Only interpolates display values. Native samples and seeks are owned by the session. */
    fun renderFrame(frameTimeNanos: Long) {
        if (isAwaitingSeek) return
        applyPlayerPosition(displayClock.positionAt(
            reportedPositionMs.toLong(), frameTimeNanos, backendPlaying, playbackSpeed,
        ).toFloat())
    }
}
