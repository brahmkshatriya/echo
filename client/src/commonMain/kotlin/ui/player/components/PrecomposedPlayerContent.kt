package dev.brahmkshatriya.echo.app.ui.player.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeLayoutState
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSheet
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private const val ExpandedPlayerSlot = "expanded-player"
private const val CompactPlayerSlot = "compact-player"

/** Prepare the hidden player in small composition slices after the first frame. */
@Composable
internal fun PrecomposedPlayerContent(
    compactContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit,
) {
    val sheet = LocalPlayerSheet.current
    val expansionRequested by remember(sheet) {
        derivedStateOf {
            sheet == null || sheet.sheetState.targetValue == SheetValue.Expanded ||
                sheet.progressState.value > 0f
        }
    }
    val layoutState = remember { SubcomposeLayoutState() }
    var prepared by remember { mutableStateOf(expansionRequested) }
    val currentExpandedContent = rememberUpdatedState(expandedContent)
    val retainedExpandedContent: @Composable () -> Unit = remember {
        { currentExpandedContent.value() }
    }

    SubcomposeLayout(layoutState, Modifier.fillMaxSize()) { constraints ->
        val children = if (prepared) {
            subcompose(ExpandedPlayerSlot, retainedExpandedContent)
        } else {
            subcompose(CompactPlayerSlot, compactContent)
        }
        val placeables = children.map { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { it.placeRelative(0, 0) }
        }
    }

    LaunchedEffect(layoutState, expansionRequested) {
        if (prepared) return@LaunchedEffect
        if (expansionRequested) {
            // An immediate user expansion takes priority over background preparation.
            prepared = true
            return@LaunchedEffect
        }
        withFrameNanos { }
        val pending = layoutState.createPausedPrecomposition(
            ExpandedPlayerSlot, retainedExpandedContent,
        )
        var applied = false
        try {
            // Changing composition locals can repeatedly invalidate paused work.
            // Bound this warm-up so it cannot keep an otherwise idle screen awake.
            repeat(120) {
                withFrameNanos { }
                val started = TimeSource.Monotonic.markNow()
                if (pending.resume { started.elapsedNow() >= 4.milliseconds }) {
                    pending.apply()
                    applied = true
                    prepared = true
                    return@LaunchedEffect
                }
            }
        } finally {
            if (!applied) pending.cancel()
        }
    }
}
