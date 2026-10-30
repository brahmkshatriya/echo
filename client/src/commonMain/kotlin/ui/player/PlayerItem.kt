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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.playback.PlayerSession
import dev.brahmkshatriya.echo.app.ui.components.ScrollbarState
import dev.brahmkshatriya.echo.app.ui.components.scrollbarStateValue
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Neutral item shell. Song items supply a theme and background color; video items can leave both
 * colors unset.
 */
@Composable
fun PlayerItemHost(
    backgroundColor: Color? = null,
    outlineColor: Color? = null,
    content: @Composable () -> Unit,
) {
    Box(Modifier.playerBackground(backgroundColor, outlineColor)) { content() }
}

const val collapsedHorizontalPadding = 8
val playerBottomBarHeight = 64.dp
val playerBottomBarControlSize = 48.dp
const val PlayerBottomBarContentType = "player-bottom-bar"
const val PlayerScrollbarThumbSizePercent = 0.16f

/**
 * Page-local buttons stay in place during skips: select the target instantly, then animate the new
 * page's visibility instead of sliding its click targets.
 */
@Stable
class PlayerSkipNavigator(
    private val pagerState: PagerState,
    private val controls: PlayerSession,
    private val scope: CoroutineScope,
) {
    private var pendingEntryId by mutableStateOf<Int?>(null)
    private var navigationJob: Job? = null
    private var requestId = 0
    private val entryAnimation = Animatable(1f)
    var enteringEntryId by mutableStateOf<Int?>(null)
        private set

    val entryProgress: Float
        get() = entryAnimation.value

    val isNavigating: Boolean
        get() = pendingEntryId != null

    private fun originEntryId(): Int = pendingEntryId ?: controls.currentEntryId

    val canGoNext: Boolean
        get() = controls.nextEntryId(originEntryId()) != null

    fun next() = navigate(controls.nextEntryId(originEntryId()))

    fun previous() {
        val target = controls.previousTarget(originEntryId(), allowRestart = !isNavigating)
        if (target == controls.currentEntryId && !isNavigating) controls.previous()
        else navigate(target)
    }

    private fun navigate(entryId: Int?) {
        val destination = entryId?.let(controls::indexOfEntry) ?: return
        if (destination !in 0 until pagerState.pageCount) return
        val id = ++requestId
        val willChangePage = pagerState.currentPage != destination
        pendingEntryId = entryId
        enteringEntryId = if (willChangePage) entryId else null
        navigationJob?.cancel()
        navigationJob =
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    if (willChangePage) entryAnimation.snapTo(0f)
                    controls.selectEntry(entryId)
                    if (willChangePage) {
                        snapshotFlow { pagerState.settledPage }.first { it == destination }
                        entryAnimation.animateTo(
                            1f,
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                        )
                    }
                } finally {
                    if (requestId == id) {
                        pendingEntryId = null
                        enteringEntryId = null
                        navigationJob = null
                    }
                }
            }
    }

    /** Selecting from the queue takes priority over any skip in progress. */
    fun selectEntry(entryId: Int) {
        val page = controls.indexOfEntry(entryId)
        if (page < 0) return
        requestId++
        pendingEntryId = null
        enteringEntryId = null
        navigationJob?.cancel()
        navigationJob = null
        controls.selectEntry(entryId)
    }
}

/**
 * Only fades and blurs pixels, never translates the page's pointer targets. Manual horizontal
 * swipes still use the normal pager slide transition.
 */
fun Modifier.animatedSkipEntrance(
    navigator: PlayerSkipNavigator?,
    entryId: Int,
    effectProgress: () -> Float = { 1f },
): Modifier = graphicsLayer {
    if (navigator?.enteringEntryId == entryId) {
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
class PlayerScrollbarItemSizes {
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

    fun fallbackSize(viewportSize: Int): Int =
        if (knownCount > 0) (knownTotal.toDouble() / knownCount).roundToInt().coerceAtLeast(1)
        else viewportSize.coerceAtLeast(1)

    fun estimatedContentHeight(totalItems: Int, fallbackSize: Int): Int {
        var total = 0L
        for (index in 0 until totalItems) total += itemSize(index, fallbackSize).toLong()
        return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun estimatedScrollOffset(
        firstVisibleIndex: Int,
        firstVisibleItemScrollOffset: Int,
        fallbackSize: Int,
    ): Int {
        var beforeFirstItem = 0L
        for (index in 0 until firstVisibleIndex) beforeFirstItem +=
            itemSize(index, fallbackSize).toLong()
        val firstItemSize = itemSize(firstVisibleIndex, fallbackSize)
        val offset = firstVisibleItemScrollOffset.coerceIn(0, firstItemSize)
        return (beforeFirstItem + offset).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun itemAtOffset(targetScrollOffset: Int, totalItems: Int, fallbackSize: Int): Long {
        var remainingOffset = targetScrollOffset
        var targetIndex = 0
        while (targetIndex < totalItems - 1) {
            val itemSize = itemSize(targetIndex, fallbackSize)
            if (remainingOffset < itemSize) break
            remainingOffset -= itemSize
            targetIndex++
        }
        return (targetIndex.toLong() shl 32) or (remainingOffset.toLong() and 0xffffffffL)
    }
}

@Composable
fun rememberPlayerScrollbarState(
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
            }
            .collect { snapshot ->
                if (snapshot.totalItems <= 0) return@collect
                listState.layoutInfo.visibleItemsInfo.forEach { item ->
                    itemSizes.update(item.index, item.size)
                }

                val viewportSize = snapshot.viewportSize.coerceAtLeast(1)
                val fallbackSize = itemSizes.fallbackSize(viewportSize)
                val totalHeight =
                    itemSizes.estimatedContentHeight(
                        totalItems = snapshot.totalItems,
                        fallbackSize = fallbackSize,
                    )
                val maxScrollOffset = (totalHeight - viewportSize).coerceAtLeast(1)
                val scrollOffset =
                    itemSizes.estimatedScrollOffset(
                        firstVisibleIndex = snapshot.firstVisibleIndex,
                        firstVisibleItemScrollOffset = snapshot.firstVisibleItemScrollOffset,
                        fallbackSize = fallbackSize,
                    )
                val maxThumbTravel = 1f - PlayerScrollbarThumbSizePercent
                val thumbMovedPercent =
                    when {
                        !snapshot.canScrollBackward -> 0f
                        else ->
                            (scrollOffset.toFloat() / maxScrollOffset * maxThumbTravel).coerceIn(
                                0f,
                                maxThumbTravel,
                            )
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
fun rememberPlayerScrollbarThumbMover(
    listState: LazyListState,
    itemSizes: PlayerScrollbarItemSizes,
): (Float) -> Unit {
    var thumbMovedPercent by remember { mutableFloatStateOf(Float.NaN) }
    LaunchedEffect(thumbMovedPercent) {
        if (thumbMovedPercent.isNaN()) return@LaunchedEffect
        val layoutInfo = listState.layoutInfo
        val totalItems = layoutInfo.totalItemsCount
        if (totalItems <= 0) return@LaunchedEffect

        val viewportSize =
            (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(1)
        val fallbackSize = itemSizes.fallbackSize(viewportSize)
        val totalHeight = itemSizes.estimatedContentHeight(totalItems, fallbackSize)
        val maxScrollOffset = (totalHeight - viewportSize).coerceAtLeast(0)
        val maxThumbTravel = 1f - PlayerScrollbarThumbSizePercent
        val targetScrollOffset =
            (maxScrollOffset * (thumbMovedPercent / maxThumbTravel).coerceIn(0f, 1f)).roundToInt()
        val target =
            itemSizes.itemAtOffset(
                targetScrollOffset = targetScrollOffset,
                totalItems = totalItems,
                fallbackSize = fallbackSize,
            )
        val targetIndex = (target ushr 32).toInt()
        val remainingOffset = target.toInt()
        listState.requestScrollToItem(targetIndex, remainingOffset)
    }
    return remember { { newPercentage -> thumbMovedPercent = newPercentage } }
}
