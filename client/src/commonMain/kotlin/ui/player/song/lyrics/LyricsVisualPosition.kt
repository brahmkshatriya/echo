package dev.brahmkshatriya.echo.app.ui.player.song.lyrics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.staticCompositionLocalOf
import dev.brahmkshatriya.echo.app.playback.PlayerTimelineState
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSheet

internal val LocalLyricsBloomStrength = staticCompositionLocalOf<State<Float>?> { null }

/**
 * Keep per-frame clock dependencies out of composition. snapshotFlow publishes
 * only changed structural values; drawing still reads the live clock directly.
 * A derivedStateOf read in composition also makes the composer revisit its
 * conditional invalidations on every clock tick, even when its value is equal.
 */
@Composable
internal fun <T> rememberLyricsStructure(
    vararg keys: Any?,
    calculation: () -> T,
): State<T> {
    val currentCalculation = rememberUpdatedState(calculation)
    val result = remember(*keys) {
        mutableStateOf(Snapshot.withoutReadObservation(calculation))
    }
    LaunchedEffect(result) {
        snapshotFlow { currentCalculation.value() }.collect { result.value = it }
    }
    return result
}

/** Hidden lyric views hold a frame, then resume from the live playback timeline. */
@Composable
internal fun rememberLyricsVisualPosition(
    timelineState: PlayerTimelineState,
    active: Boolean,
): State<Long> {
    val currentActive = rememberUpdatedState(active)
    val heldPosition = remember(timelineState, active) {
        Snapshot.withoutReadObservation { timelineState.positionMs.toLong() }
    }
    val currentHeldPosition = rememberUpdatedState(heldPosition)
    // Keep the clock identity when hiding a page. Replacing it invalidates
    // every lyric row and rebuilds their timing observers during the fade.
    return remember(timelineState) {
        derivedStateOf {
            if (currentActive.value) timelineState.positionMs.toLong()
            else currentHeldPosition.value
        }
    }
}

/** Reserve transition frames for moving the player; restore the decorative halo at rest. */
@Composable
internal fun rememberLyricsBloomStrength(): State<Float> {
    LocalLyricsBloomStrength.current?.let { return it }
    val sheet = LocalPlayerSheet.current
    val settledExpanded by remember(sheet) {
        derivedStateOf {
            sheet == null || (!sheet.sheetState.isAnimationRunning && sheet.progressState.value >= 0.999f)
        }
    }
    return animateFloatAsState(
        targetValue = if (settledExpanded) 1f else 0f,
        animationSpec = tween(120),
        label = "Player transition lyric glow",
    )
}
