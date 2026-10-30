package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.runtime.*
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.brahmkshatriya.echo.app.ui.player.PlayerScrollbarItemSizes
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private object PlayerHeroVisibleInfo : NavigationEventInfo()

private object PlayerHeroHiddenInfo : NavigationEventInfo()

/** Gesture state lives with its page and shares the page's list geometry. */
private class SongPlayerBackState(
    private val listState: LazyListState,
    private val scrollbarItemSizes: PlayerScrollbarItemSizes,
) {
    var active by mutableStateOf(false)
        private set

    var startIndex = 0
        private set

    var startItemOffset = 0
        private set

    private var startScrollOffset = 0
    private var fallbackSize = 1
    private var totalItems = 0

    fun finish() {
        startScrollOffset = 0
        active = false
    }

    fun capture(isBottomBarSticky: Boolean): Boolean {
        val layoutInfo = listState.layoutInfo
        val itemCount = layoutInfo.totalItemsCount
        if (itemCount <= 0 || !isBottomBarSticky || !listState.canScrollBackward) return false

        layoutInfo.visibleItemsInfo.forEach { item ->
            scrollbarItemSizes.update(item.index, item.size)
        }
        val viewportSize =
            (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(1)
        val estimatedSize = scrollbarItemSizes.fallbackSize(viewportSize)
        startIndex = listState.firstVisibleItemIndex
        startItemOffset = listState.firstVisibleItemScrollOffset
        startScrollOffset =
            scrollbarItemSizes.estimatedScrollOffset(
                firstVisibleIndex = startIndex,
                firstVisibleItemScrollOffset = startItemOffset,
                fallbackSize = estimatedSize,
            )
        fallbackSize = estimatedSize
        totalItems = itemCount
        active = startScrollOffset > 0
        return active
    }

    fun applyProgress(progress: Float, heroBackMaxPredictiveTravelPx: Int) {
        if (!active || totalItems <= 0) return
        val predictiveTravel = minOf(startScrollOffset, heroBackMaxPredictiveTravelPx)
        val targetScrollOffset =
            (startScrollOffset - predictiveTravel * progress.coerceIn(0f, 1f))
                .roundToInt()
                .coerceAtLeast(0)
        val target =
            scrollbarItemSizes.itemAtOffset(
                targetScrollOffset = targetScrollOffset,
                totalItems = totalItems,
                fallbackSize = fallbackSize,
            )
        listState.requestScrollToItem(
            index = (target ushr 32).toInt(),
            scrollOffset = target.toInt(),
        )
    }
}

@Composable
fun SongPlayerBackHandler(
    entryId: Int,
    listState: LazyListState,
    scrollbarItemSizes: PlayerScrollbarItemSizes,
    isCurrentItem: Boolean,
    selectorMounted: Boolean,
    isBottomBarSticky: Boolean,
    viewportHeightPx: Int,
) {
    val scope = rememberCoroutineScope()
    val heroBackProgress = remember(entryId) { Animatable(0f) }
    val heroBackAnimationSpec = motionScheme.defaultSpatialSpec<Float>()
    val backState =
        remember(entryId, listState, scrollbarItemSizes) {
            SongPlayerBackState(listState, scrollbarItemSizes)
        }
    val heroBackMaxPredictiveTravelPx = (viewportHeightPx / 4).coerceAtLeast(1)

    val heroBackEnabled =
        isCurrentItem && !selectorMounted && (isBottomBarSticky || backState.active)
    val heroBackNavigationEventState =
        rememberNavigationEventState(
            currentInfo = if (heroBackEnabled) PlayerHeroHiddenInfo else PlayerHeroVisibleInfo,
            backInfo = if (heroBackEnabled) listOf(PlayerHeroVisibleInfo) else emptyList(),
        )
    if (LocalNavigationEventDispatcherOwner.current != null)
        NavigationBackHandler(
            state = heroBackNavigationEventState,
            isBackEnabled = heroBackEnabled,
            onBackCompleted = {
                scope.launch {
                    listState.animateScrollToItem(0)
                    backState.finish()
                    heroBackProgress.snapTo(0f)
                }
            },
            onBackCancelled = {
                scope.launch {
                    heroBackProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = heroBackAnimationSpec,
                    )
                    listState.scrollToItem(backState.startIndex, backState.startItemOffset)
                    backState.finish()
                }
            },
        )
    LaunchedEffect(heroBackNavigationEventState, heroBackProgress, isBottomBarSticky) {
        snapshotFlow {
                when (val state = heroBackNavigationEventState.transitionState) {
                    NavigationEventTransitionState.Idle -> null
                    is NavigationEventTransitionState.InProgress -> state.latestEvent.progress
                }
            }
            .collectLatest { progress ->
                if (progress == null) return@collectLatest
                if (!backState.active && !backState.capture(isBottomBarSticky)) return@collectLatest
                heroBackProgress.snapTo(progress.coerceIn(0f, 1f))
            }
    }
    LaunchedEffect(heroBackProgress, heroBackMaxPredictiveTravelPx) {
        snapshotFlow { heroBackProgress.value }
            .collectLatest { progress ->
                backState.applyProgress(progress, heroBackMaxPredictiveTravelPx)
            }
    }
}
