package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.ui.components.ScrollbarState
import dev.brahmkshatriya.echo.app.ui.components.scrollbarStateValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Neutral item shell. Song items supply a theme and background color;
 * video items can leave both colors unset. */
@Composable
internal fun PlayerItemHost(
    backgroundColor: Color? = null,
    outlineColor: Color? = null,
    content: @Composable () -> Unit,
) {
    Box(Modifier.playerBackground(backgroundColor, outlineColor)) { content() }
}


const val collapsedHorizontalPadding = 8
internal val playerBottomBarHeight = 64.dp
internal val playerBottomBarControlSize = 48.dp
internal const val PlayerBottomBarContentType = "player-bottom-bar"
internal const val PlayerScrollbarThumbSizePercent = 0.16f

@Stable
internal class PlayerTimelineState(val durationMs: Float) {
    var positionMs by mutableFloatStateOf(0f)
    var isSeeking by mutableStateOf(false)
}

@Composable
internal fun rememberPlayerTimelineState(
    key: Any?,
    durationMs: Float,
    running: Boolean,
    onFinished: suspend () -> Boolean = { false },
): PlayerTimelineState {
    val state = remember(key, durationMs) { PlayerTimelineState(durationMs) }
    val currentOnFinished by rememberUpdatedState(onFinished)
    val isSeeking = state.isSeeking

    LaunchedEffect(state, running, isSeeking) {
        if (!running || isSeeking || state.positionMs >= state.durationMs) {
            return@LaunchedEffect
        }

        var previousFrameNanos = withFrameNanos { it }
        while (isActive) {
            val frameNanos = withFrameNanos { it }
            val elapsedMs = (frameNanos - previousFrameNanos) / 1_000_000f
            state.positionMs =
                (state.positionMs + elapsedMs).coerceAtMost(state.durationMs)
            if (state.positionMs >= state.durationMs) {
                state.positionMs = 0f
                if (currentOnFinished()) return@LaunchedEffect
            }
            previousFrameNanos = frameNanos
        }
    }

    return state
}

internal val LocalPlayerTimelineState = staticCompositionLocalOf<PlayerTimelineState?> { null }

/** Page-local buttons stay in place during skips: select the target instantly,
 * then animate the new page's visibility instead of sliding its click targets. */
@Stable
internal class PlayerSkipNavigator(
    private val pagerState: PagerState,
    private val controls: PlayerControlsState,
    private val scope: CoroutineScope,
) {
    private var pendingSong by mutableStateOf<Int?>(null)
    private var navigationJob: Job? = null
    private var requestId = 0
    private val entryAnimation = Animatable(1f)
    var enteringSong by mutableStateOf<Int?>(null)
        private set
    val entryProgress: Float get() = entryAnimation.value

    val isNavigating: Boolean get() = pendingSong != null
    private fun originSong(): Int = pendingSong ?: controls.songAt(pagerState.targetPage)
    val canGoNext: Boolean get() = controls.nextSong(originSong()) != null

    fun next() = navigate(controls.nextSong(originSong()))
    fun previous() = navigate(controls.previousSong(originSong()))

    private fun navigate(song: Int?) {
        val destination = song?.let(controls::pageOf) ?: return
        if (destination !in 0 until pagerState.pageCount) return
        val id = ++requestId
        val willChangePage = pagerState.currentPage != destination
        pendingSong = song
        controls.currentSong = song
        enteringSong = if (willChangePage) song else null
        navigationJob?.cancel()
        navigationJob = scope.launch {
            try {
                if (willChangePage) {
                    entryAnimation.snapTo(0f)
                }
                pagerState.scrollToPage(destination)
                if (willChangePage) {
                    entryAnimation.animateTo(
                        1f,
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                    )
                }
            } finally {
                if (requestId == id) {
                    pendingSong = null
                    enteringSong = null
                    navigationJob = null
                }
            }
        }
    }

    /** Selecting from the queue takes priority over any skip in progress. */
    fun selectSong(song: Int) {
        val page = controls.pageOf(song)
        if (page < 0) return
        requestId++
        pendingSong = null
        enteringSong = null
        navigationJob?.cancel()
        navigationJob = null
        controls.currentSong = song
        pagerState.requestScrollToPage(page)
    }
}

/** Only fades and blurs pixels, never translates the page's pointer targets.
 * Manual horizontal swipes still use the normal pager slide transition. */
internal fun Modifier.animatedSkipEntrance(
    navigator: PlayerSkipNavigator?,
    song: Int,
    effectProgress: () -> Float = { 1f },
): Modifier = graphicsLayer {
    if (navigator?.enteringSong == song) {
        // Fade on every skip, including when collapsed. Only blur is gated
        // by sheet expansion so the mini-player remains sharp.
        val remaining = (1f - navigator.entryProgress).coerceIn(0f, 1f)
        alpha = 1f - remaining * 0.28f
        val blur = 18.dp.toPx() * remaining * effectProgress().coerceIn(0f, 1f)
        renderEffect = if (blur > 0.5f) BlurEffect(blur, blur, TileMode.Decal) else null
    } else {
        alpha = 1f
        renderEffect = null
    }
}

@Stable
internal class PlayerScrollbarItemSizes {
    private var sizes = IntArray(16)
    private var knownCount = 0
    private var knownTotal = 0L

    private fun ensureCapacity(index: Int) {
        if (index < sizes.size) return
        var newSize = sizes.size
        while (newSize <= index) newSize *= 2
        sizes = sizes.copyOf(newSize)
    }

    fun update(index: Int, size: Int) {
        if (index < 0 || size <= 0) return
        ensureCapacity(index)
        val previous = sizes[index]
        if (previous == size) return
        if (previous == 0) knownCount++ else knownTotal -= previous.toLong()
        sizes[index] = size
        knownTotal += size.toLong()
    }

    fun itemSize(index: Int, fallbackSize: Int): Int =
        sizes.getOrNull(index)?.takeIf { it > 0 } ?: fallbackSize

    fun fallbackSize(viewportSize: Int): Int = if (knownCount > 0) {
        (knownTotal.toDouble() / knownCount).roundToInt().coerceAtLeast(1)
    } else {
        viewportSize.coerceAtLeast(1)
    }

    fun estimatedContentHeight(totalItems: Int, fallbackSize: Int): Int {
        var total = 0L
        for (index in 0 until totalItems) {
            total += itemSize(index, fallbackSize).toLong()
        }
        return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun estimatedScrollOffset(
        firstVisibleIndex: Int,
        firstVisibleItemScrollOffset: Int,
        fallbackSize: Int,
    ): Int {
        var beforeFirstItem = 0L
        for (index in 0 until firstVisibleIndex) {
            beforeFirstItem += itemSize(index, fallbackSize).toLong()
        }
        val firstItemSize = itemSize(firstVisibleIndex, fallbackSize)
        val offset = firstVisibleItemScrollOffset.coerceIn(0, firstItemSize)
        return (beforeFirstItem + offset).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun itemAtOffset(
        targetScrollOffset: Int,
        totalItems: Int,
        fallbackSize: Int,
    ): Long {
        var remainingOffset = targetScrollOffset
        var targetIndex = 0
        while (targetIndex < totalItems - 1) {
            val itemSize = itemSize(targetIndex, fallbackSize)
            if (remainingOffset < itemSize) break
            remainingOffset -= itemSize
            targetIndex++
        }
        return (targetIndex.toLong() shl 32) or
                (remainingOffset.toLong() and 0xffffffffL)
    }
}

@Composable
internal fun rememberPlayerScrollbarState(
    listState: LazyListState,
    itemSizes: PlayerScrollbarItemSizes,
): ScrollbarState {
    val state = remember { ScrollbarState() }
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            var visibleItemsSignature = 1
            layoutInfo.visibleItemsInfo.forEach { item ->
                visibleItemsSignature = 31 * visibleItemsSignature + item.index
                visibleItemsSignature = 31 * visibleItemsSignature + item.size
            }
            PlayerScrollbarSnapshot(
                firstVisibleIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                canScrollBackward = listState.canScrollBackward,
                totalItems = layoutInfo.totalItemsCount,
                viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset,
                visibleItemsSignature = visibleItemsSignature,
            )
        }.collect { snapshot ->
            if (snapshot.totalItems <= 0) return@collect
            listState.layoutInfo.visibleItemsInfo.forEach { item ->
                itemSizes.update(item.index, item.size)
            }

            val viewportSize = snapshot.viewportSize.coerceAtLeast(1)
            val fallbackSize = itemSizes.fallbackSize(viewportSize)
            val totalHeight = itemSizes.estimatedContentHeight(
                totalItems = snapshot.totalItems,
                fallbackSize = fallbackSize,
            )
            val maxScrollOffset = (totalHeight - viewportSize).coerceAtLeast(1)
            val scrollOffset = itemSizes.estimatedScrollOffset(
                firstVisibleIndex = snapshot.firstVisibleIndex,
                firstVisibleItemScrollOffset = snapshot.firstVisibleItemScrollOffset,
                fallbackSize = fallbackSize,
            )
            val maxThumbTravel = 1f - PlayerScrollbarThumbSizePercent
            val thumbMovedPercent = when {
                !snapshot.canScrollBackward -> 0f
                else -> (scrollOffset.toFloat() / maxScrollOffset * maxThumbTravel)
                    .coerceIn(0f, maxThumbTravel)
            }
            state.onScroll(
                scrollbarStateValue(
                    thumbSizePercent = PlayerScrollbarThumbSizePercent,
                    thumbMovedPercent = thumbMovedPercent,
                )
            )
        }
    }
    return state
}

private data class PlayerScrollbarSnapshot(
    val firstVisibleIndex: Int,
    val firstVisibleItemScrollOffset: Int,
    val canScrollBackward: Boolean,
    val totalItems: Int,
    val viewportSize: Int,
    val visibleItemsSignature: Int,
)

@Composable
internal fun rememberPlayerScrollbarThumbMover(
    listState: LazyListState,
    itemSizes: PlayerScrollbarItemSizes,
): (Float) -> Unit {
    var thumbMovedPercent by remember { mutableFloatStateOf(Float.NaN) }
    LaunchedEffect(thumbMovedPercent) {
        if (thumbMovedPercent.isNaN()) return@LaunchedEffect
        val layoutInfo = listState.layoutInfo
        val totalItems = layoutInfo.totalItemsCount
        if (totalItems <= 0) return@LaunchedEffect

        val viewportSize = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset)
            .coerceAtLeast(1)
        val fallbackSize = itemSizes.fallbackSize(viewportSize)
        val totalHeight = itemSizes.estimatedContentHeight(totalItems, fallbackSize)
        val maxScrollOffset = (totalHeight - viewportSize).coerceAtLeast(0)
        val maxThumbTravel = 1f - PlayerScrollbarThumbSizePercent
        val targetScrollOffset = (maxScrollOffset *
                (thumbMovedPercent / maxThumbTravel).coerceIn(0f, 1f))
            .roundToInt()
        val target = itemSizes.itemAtOffset(
            targetScrollOffset = targetScrollOffset,
            totalItems = totalItems,
            fallbackSize = fallbackSize,
        )
        val targetIndex = (target ushr 32).toInt()
        val remainingOffset = target.toInt()
        listState.requestScrollToItem(targetIndex, remainingOffset)
    }
    return remember {
        { newPercentage -> thumbMovedPercent = newPercentage }
    }
}
