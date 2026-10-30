package dev.brahmkshatriya.echo.app.ui.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import dev.brahmkshatriya.echo.app.playback.PlayerTimelineState

/** Observe the live duration as well as position, publishing only displayed seconds. */
@Composable
fun rememberTimelineLabels(
    timeline: PlayerTimelineState,
    showRemainingTime: Boolean,
): State<Pair<String, String>> {
    fun seconds(): Pair<Int, Int> {
        val position = timeline.positionMs
        val end = if (showRemainingTime) timeline.durationMs - position else timeline.durationMs
        return (position / 1_000).toInt().coerceAtLeast(0) to
            (end / 1_000).toInt().coerceAtLeast(0)
    }
    fun labels(value: Pair<Int, Int>): Pair<String, String> =
        formatTime(value.first * 1_000f) to
            (if (showRemainingTime) "-" else "") + formatTime(value.second * 1_000f)

    return produceState(
        initialValue = Snapshot.withoutReadObservation { labels(seconds()) },
        timeline, showRemainingTime,
    ) {
        snapshotFlow { seconds() }.collect { value = labels(it) }
    }
}
