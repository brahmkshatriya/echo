package dev.brahmkshatriya.echo.app.playback

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive

/**
 * Attaches presentation to an existing service session; disposal only stops display interpolation.
 */
@Composable
fun ProvidePlaybackSession(
    session: PlayerSession,
    pagerState: PagerState,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(session, pagerState) {
        var initial = true
        snapshotFlow { session.selection }
            .collectLatest { selection ->
                val page = session.indexOfEntry(selection.entryId)
                if (page < 0) return@collectLatest
                if (!initial && selection.reason == SelectionReason.Automatic)
                    pagerState.animateScrollToPage(
                        page,
                        animationSpec = tween(450, easing = FastOutSlowInEasing),
                    )
                else if (pagerState.settledPage != page) pagerState.scrollToPage(page)
                initial = false
            }
    }
    LaunchedEffect(session, pagerState) {
        pagerState.interactionSource.interactions
            .filterIsInstance<androidx.compose.foundation.interaction.DragInteraction.Start>()
            .collectLatest {
                val revision = session.selection.revision
                snapshotFlow { pagerState.isScrollInProgress }.first { it }
                snapshotFlow { pagerState.isScrollInProgress }.first { !it }
                if (session.selection.revision == revision)
                    session
                        .entryIdAt(pagerState.settledPage)
                        .takeIf { it >= 0 }
                        ?.let { session.selectEntry(it) }
            }
    }
    LaunchedEffect(session, session.currentEntryId, session.isPlaying) {
        val timeline = session.activeTimeline?.second ?: return@LaunchedEffect
        if (!session.isPlaying) return@LaunchedEffect
        while (isActive) withFrameNanos { timeline.renderFrame(it) }
    }
    content()
}
