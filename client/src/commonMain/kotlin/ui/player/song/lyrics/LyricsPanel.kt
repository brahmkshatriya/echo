package dev.brahmkshatriya.echo.app.ui.player.song.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerControls
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerTimelineState
import dev.brahmkshatriya.echo.app.ui.player.PlayerTimelineState
import echo.client.generated.resources.Res
import echo.client.generated.resources.ic_keyboard_arrow_down
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private const val FullLyricsAnchorRatio = 0.35f
private const val FullLyricsMotionDurationMs = 750
private const val FullLyricsManualResyncDurationMs = 420
private const val FullLyricsStaggerPerLineMs = 20
private const val FullLyricsMaxStaggerMs = 200
private const val FullLyricsAnimatedItemRadius = 12
private const val FullLyricsRenderBufferViewports = 1f
private val FullLyricsItemGap = 16.dp
private val FullLyricsFallbackHeight = 68.dp
internal val FullLyricsGapIndicatorHeight = 72.dp

private sealed interface FullLyricsDisplayItem {
    data class Line(val lineIndex: Int) : FullLyricsDisplayItem

    data class Gap(
        val afterLineIndex: Int,
        val startMs: Long,
        val endMs: Long,
    ) : FullLyricsDisplayItem
}

private val FullLyricsDisplayItem.isGap: Boolean
    get() = this is FullLyricsDisplayItem.Gap

private fun FloatArray.lowerBound(value: Float): Int {
    var low = 0
    var high = size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (this[mid] < value) low = mid + 1 else high = mid
    }
    return low
}

private fun FloatArray.upperBound(value: Float): Int {
    var low = 0
    var high = size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (this[mid] <= value) low = mid + 1 else high = mid
    }
    return low
}

private fun fullLyricsTransitionOffset(
    startOffset: Float,
    elapsedMs: Float,
    distanceFromAnchor: Int,
    seeking: Boolean,
): Float {
    if (startOffset == 0f || distanceFromAnchor > FullLyricsAnimatedItemRadius) return 0f
    val delayMs = if (seeking) 0 else {
        (distanceFromAnchor * FullLyricsStaggerPerLineMs).coerceAtMost(FullLyricsMaxStaggerMs)
    }
    val durationMs = if (seeking) 140 else FullLyricsMotionDurationMs
    val progress = ((elapsedMs - delayMs).coerceAtLeast(0f) / durationMs.toFloat())
        .coerceIn(0f, 1f)
    return startOffset * (1f - FastOutSlowInEasing.transform(progress))
}

@Composable
internal fun LyricsPanel(
    lyrics: Lyrics,
    timingIndex: LyricsTimingIndex? = null,
    userScrollEnabled: Boolean,
    chromeCollapsed: Boolean = false,
    onChromeInteraction: () -> Unit = {},
    onViewportMotionChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val timelineState = LocalPlayerTimelineState.current ?: return
    val isPlaying = LocalPlayerControls.current?.isPlaying != false
    val contentModifier = modifier.padding(horizontal = 12.dp)
    when (lyrics) {
        is Lyrics.Simple -> BoxWithConstraints(
            modifier = contentModifier.fillMaxSize(),
        ) {
            val listState = rememberLazyListState()
            val currentOnViewportMotionChange by rememberUpdatedState(onViewportMotionChange)
            val currentOnChromeInteraction by rememberUpdatedState(onChromeInteraction)
            LaunchedEffect(listState) {
                var previousIndex = listState.firstVisibleItemIndex
                var previousOffset = listState.firstVisibleItemScrollOffset
                snapshotFlow {
                    listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                }.collectLatest { (index, offset) ->
                    if (index != previousIndex || offset != previousOffset) {
                        val scrollingDown = index > previousIndex ||
                                (index == previousIndex && offset > previousOffset)
                        if (scrollingDown) {
                            currentOnChromeInteraction()
                        } else {
                            currentOnViewportMotionChange(true)
                        }
                    }
                    previousIndex = index
                    previousOffset = offset
                }
            }
            val verticalContentPadding = if (constraints.hasBoundedHeight) maxHeight / 2 else 64.dp
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .lyricsEdgeFade(),
                state = listState,
                userScrollEnabled = userScrollEnabled,
                contentPadding = PaddingValues(vertical = verticalContentPadding),
            ) {
                item {
                    Text(
                        text = lyrics.text,
                        modifier = Modifier.fillMaxWidth(),
                        style = typography.headlineMedium,
                        color = colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        is Lyrics.Line -> FullTimedLyrics(
            timingIndex = timingIndex
                ?: remember(lyrics.lines) { LyricsTimingIndex.line(lyrics.lines) },
            timelineState = timelineState,
            isPlaying = isPlaying,
            userScrollEnabled = userScrollEnabled,
            chromeCollapsed = chromeCollapsed,
            onChromeInteraction = onChromeInteraction,
            onViewportMotionChange = onViewportMotionChange,
            modifier = contentModifier,
        )

        is Lyrics.Word -> FullTimedLyrics(
            timingIndex = timingIndex
                ?: remember(lyrics.lines) { LyricsTimingIndex.word(lyrics.lines) },
            timelineState = timelineState,
            isPlaying = isPlaying,
            userScrollEnabled = userScrollEnabled,
            chromeCollapsed = chromeCollapsed,
            onChromeInteraction = onChromeInteraction,
            onViewportMotionChange = onViewportMotionChange,
            modifier = contentModifier,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FullTimedLyrics(
    timingIndex: LyricsTimingIndex,
    timelineState: PlayerTimelineState,
    isPlaying: Boolean,
    userScrollEnabled: Boolean,
    chromeCollapsed: Boolean,
    onChromeInteraction: () -> Unit,
    onViewportMotionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var autoScrollSuppressed by remember(timingIndex) { mutableStateOf(false) }
    var followLineIndex by remember(timingIndex) { mutableIntStateOf(0) }
    var contentOffsetPx by remember(timingIndex) { mutableFloatStateOf(Float.NaN) }
    var transitionStartOffsets by remember(timingIndex) { mutableStateOf(FloatArray(0)) }
    var transitionElapsedMs by remember(timingIndex) { mutableFloatStateOf(0f) }
    var transitionAnchorDisplayIndex by remember(timingIndex) { mutableIntStateOf(0) }
    var transitionSeeking by remember(timingIndex) { mutableStateOf(false) }
    var transitionGeneration by remember(timingIndex) { mutableIntStateOf(0) }
    var manualResyncNonce by remember(timingIndex) { mutableIntStateOf(0) }
    var directManualResyncActive by remember(timingIndex) { mutableStateOf(false) }
    var isInitialLayout by remember(timingIndex) { mutableStateOf(true) }
    val itemHeights = remember(timingIndex) { mutableStateMapOf<Int, Int>() }
    val currentOnViewportMotionChange by rememberUpdatedState(onViewportMotionChange)
    val currentOnChromeInteraction by rememberUpdatedState(onChromeInteraction)
    val displayItems = remember(timingIndex.lines) {
        buildList {
            timingIndex.waitingGapBeforeFirst()?.let { gap ->
                add(
                    FullLyricsDisplayItem.Gap(
                        afterLineIndex = gap.afterLineIndex,
                        startMs = gap.startMs,
                        endMs = gap.endMs,
                    )
                )
            }
            timingIndex.lines.forEachIndexed { lineIndex, _ ->
                add(FullLyricsDisplayItem.Line(lineIndex))
                timingIndex.waitingGapAfter(lineIndex)?.let { gap ->
                    add(
                        FullLyricsDisplayItem.Gap(
                            afterLineIndex = gap.afterLineIndex,
                            startMs = gap.startMs,
                            endMs = gap.endMs,
                        )
                    )
                }
            }
        }
    }
    val lineDisplayIndices = remember(displayItems, timingIndex.lines.size) {
        IntArray(timingIndex.lines.size) { -1 }.also { indices ->
            displayItems.forEachIndexed { displayIndex, item ->
                if (item is FullLyricsDisplayItem.Line) {
                    indices[item.lineIndex] = displayIndex
                }
            }
        }
    }
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentLineIndex by remember(timingIndex, timelineState) {
        derivedStateOf { timingIndex.currentLineIndex(timelineState.positionMs.toLong()) }
    }
    val scrollTargetIndex by remember(timingIndex, timelineState) {
        derivedStateOf { timingIndex.scrollTargetIndex(timelineState.positionMs.toLong()) }
    }
    val activeLineIndex by remember(timingIndex, timelineState) {
        derivedStateOf { timingIndex.activeLineIndex(timelineState.positionMs.toLong()) }
    }
    val latestCompletedLineIndex by remember(timingIndex, timelineState) {
        derivedStateOf { timingIndex.latestCompletedLineIndex(timelineState.positionMs.toLong()) }
    }
    val isSeeking = timelineState.isSeeking

    LaunchedEffect(timingIndex) {
        withFrameNanos { }
        withFrameNanos { }
        isInitialLayout = false
    }

    LaunchedEffect(userScrollEnabled) {
        if (!userScrollEnabled) {
            autoScrollSuppressed = false
        }
    }

    LaunchedEffect(transitionGeneration) {
        if (transitionGeneration == 0 || transitionStartOffsets.isEmpty()) return@LaunchedEffect
        val generation = transitionGeneration
        val totalDurationMs =
            if (transitionSeeking) 140 else FullLyricsMotionDurationMs + FullLyricsMaxStaggerMs
        val animation = Animatable(0f)
        animation.animateTo(
            targetValue = totalDurationMs.toFloat(),
            animationSpec = tween(totalDurationMs, easing = LinearEasing),
        ) {
            if (transitionGeneration == generation) transitionElapsedMs = value
        }
        if (transitionGeneration == generation) transitionElapsedMs = totalDurationMs.toFloat()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val viewportHeightPx = constraints.maxHeight.toFloat()
        // Chrome collapse grows this viewport. Freeze the active-line anchor at the
        // 35% position from the first layout so hiding/showing chrome cannot move it.
        val anchorY = remember(timingIndex) {
            viewportHeightPx * FullLyricsAnchorRatio
        }
        val itemGapPx = with(density) { FullLyricsItemGap.toPx() }
        val fallbackHeightPx = with(density) { FullLyricsFallbackHeight.toPx() }
        val gapIndicatorHeightPx = with(density) { FullLyricsGapIndicatorHeight.toPx() }
        val topFadePx = with(density) { 130.dp.toPx() }
        val bottomFadePx = with(density) { 160.dp.toPx() }
        val heightSnapshot = itemHeights.toMap()
        val absolutePositions = remember(
            displayItems,
            heightSnapshot,
            itemGapPx,
            fallbackHeightPx,
            gapIndicatorHeightPx,
        ) {
            FloatArray(displayItems.size).also { offsets ->
                var y = 0f
                displayItems.indices.forEach { index ->
                    offsets[index] = y
                    val height = heightSnapshot[index]?.toFloat()
                        ?: if (displayItems[index].isGap) gapIndicatorHeightPx
                        else fallbackHeightPx
                    val nextItem = displayItems.getOrNull(index + 1)
                    val gap = if (
                        nextItem == null || displayItems[index].isGap || nextItem.isGap
                    ) 0f else itemGapPx
                    y += height + gap
                }
            }
        }
        val targetDisplayIndex = lineDisplayIndices
            .getOrElse(scrollTargetIndex.coerceAtLeast(0)) { -1 }
            .coerceAtLeast(0)
        val targetContentOffsetPx = anchorY - absolutePositions.getOrElse(targetDisplayIndex) { 0f }

        LaunchedEffect(
            scrollTargetIndex,
            targetContentOffsetPx,
            autoScrollSuppressed,
            isSeeking,
            isInitialLayout,
        ) {
            if (scrollTargetIndex < 0) return@LaunchedEffect
            if (autoScrollSuppressed && !isSeeking) return@LaunchedEffect
            if (isSeeking) autoScrollSuppressed = false

            val oldContentOffset = if (contentOffsetPx.isNaN()) {
                targetContentOffsetPx
            } else {
                contentOffsetPx
            }
            val commonDelta = oldContentOffset - targetContentOffsetPx

            if (isInitialLayout || contentOffsetPx.isNaN()) {
                directManualResyncActive = false
                contentOffsetPx = targetContentOffsetPx
                followLineIndex = scrollTargetIndex
                transitionStartOffsets = FloatArray(displayItems.size)
                transitionElapsedMs = 0f
                transitionAnchorDisplayIndex = targetDisplayIndex
                return@LaunchedEffect
            }

            if (isSeeking) {
                directManualResyncActive = false
                transitionStartOffsets = FloatArray(displayItems.size)
                transitionElapsedMs = 0f
                transitionAnchorDisplayIndex = targetDisplayIndex
                transitionSeeking = true
                contentOffsetPx = targetContentOffsetPx
                followLineIndex = scrollTargetIndex
                return@LaunchedEffect
            }

            if (directManualResyncActive) {
                transitionStartOffsets = FloatArray(displayItems.size)
                transitionElapsedMs = 0f
                transitionAnchorDisplayIndex = targetDisplayIndex
                transitionSeeking = false
                followLineIndex = scrollTargetIndex

                val scrollAnimation = Animatable(oldContentOffset)
                scrollAnimation.animateTo(
                    targetValue = targetContentOffsetPx,
                    animationSpec = tween(
                        durationMillis = FullLyricsManualResyncDurationMs,
                        easing = FastOutSlowInEasing,
                    ),
                ) {
                    contentOffsetPx = value
                }
                contentOffsetPx = targetContentOffsetPx
                directManualResyncActive = false
                return@LaunchedEffect
            }

            if (followLineIndex == scrollTargetIndex && abs(commonDelta) < 0.5f) {
                contentOffsetPx = targetContentOffsetPx
                return@LaunchedEffect
            }

            val previousStarts = transitionStartOffsets
            val previousElapsed = transitionElapsedMs
            val previousAnchor = transitionAnchorDisplayIndex
            val previousSeeking = transitionSeeking
            val newStarts = FloatArray(displayItems.size) { displayIndex ->
                commonDelta + fullLyricsTransitionOffset(
                    startOffset = previousStarts.getOrElse(displayIndex) { 0f },
                    elapsedMs = previousElapsed,
                    distanceFromAnchor = abs(displayIndex - previousAnchor),
                    seeking = previousSeeking,
                )
            }

            transitionStartOffsets = newStarts
            transitionElapsedMs = 0f
            transitionAnchorDisplayIndex = targetDisplayIndex
            transitionSeeking = isSeeking
            contentOffsetPx = targetContentOffsetPx
            followLineIndex = scrollTargetIndex
            transitionGeneration++
        }

        val manualOverscrollPx = viewportHeightPx * 0.12f
        val scrollClampMin = if (absolutePositions.isEmpty()) 0f
        else anchorY - absolutePositions.last() - manualOverscrollPx
        val scrollClampMax = anchorY + manualOverscrollPx

        LaunchedEffect(scrollClampMin, scrollClampMax) {
            if (!contentOffsetPx.isNaN()) {
                contentOffsetPx = contentOffsetPx.coerceIn(scrollClampMin, scrollClampMax)
            }
        }

        val renderedItemRange by remember(
            displayItems,
            absolutePositions,
            viewportHeightPx,
            targetContentOffsetPx,
        ) {
            derivedStateOf {
                if (absolutePositions.isEmpty()) displayItems.indices else {
                    val baseOffset = if (contentOffsetPx.isNaN()) {
                        targetContentOffsetPx
                    } else {
                        contentOffsetPx
                    }
                    val bufferPx = viewportHeightPx * FullLyricsRenderBufferViewports
                    val contentTop = -bufferPx - baseOffset
                    val contentBottom = viewportHeightPx + bufferPx - baseOffset
                    val first = (absolutePositions.lowerBound(contentTop) - 1)
                        .coerceIn(0, displayItems.lastIndex)
                    val last = (absolutePositions.upperBound(contentBottom) + 1)
                        .coerceIn(first, displayItems.lastIndex)
                    first..last
                }
            }
        }

        val manualScrollState = rememberScrollableState { delta ->
            if (!userScrollEnabled || absolutePositions.isEmpty()) {
                0f
            } else {
                autoScrollSuppressed = true
                directManualResyncActive = false
                if (abs(delta) > 0.5f) {
                    // A swipe up moves the lyrics toward later lines (scrolling down).
                    // Reveal the chrome in that direction; scrolling back up may hide it.
                    if (delta < 0f) {
                        currentOnChromeInteraction()
                    } else if (delta > 0f) {
                        currentOnViewportMotionChange(true)
                    }
                }
                val previousOffset = if (contentOffsetPx.isNaN()) {
                    targetContentOffsetPx
                } else {
                    contentOffsetPx
                }
                val nextOffset = (previousOffset + delta).coerceIn(scrollClampMin, scrollClampMax)
                contentOffsetPx = nextOffset
                nextOffset - previousOffset
            }
        }

        LaunchedEffect(manualScrollState, autoScrollSuppressed, manualResyncNonce) {
            if (!autoScrollSuppressed) return@LaunchedEffect

            snapshotFlow { manualScrollState.isScrollInProgress }
                .collectLatest { isScrolling ->
                    if (isScrolling) return@collectLatest

                    delay(3_000.milliseconds)
                    if (!currentIsPlaying) {
                        snapshotFlow { currentIsPlaying }.first { it }
                    }

                    if (autoScrollSuppressed) {
                        currentOnViewportMotionChange(true)
                        directManualResyncActive = true
                        autoScrollSuppressed = false
                    }
                }
        }

        val syncArrowPointsUp by remember(
            absolutePositions,
            lineDisplayIndices,
            currentLineIndex,
            anchorY,
            targetContentOffsetPx,
        ) {
            derivedStateOf {
                if (absolutePositions.isEmpty()) {
                    false
                } else {
                    val currentDisplayIndex = lineDisplayIndices
                        .getOrElse(currentLineIndex) { -1 }
                        .coerceAtLeast(0)
                    val baseOffset = if (contentOffsetPx.isNaN()) {
                        targetContentOffsetPx
                    } else {
                        contentOffsetPx
                    }
                    val transitionOffset = fullLyricsTransitionOffset(
                        startOffset = transitionStartOffsets.getOrElse(currentDisplayIndex) { 0f },
                        elapsedMs = transitionElapsedMs,
                        distanceFromAnchor = abs(currentDisplayIndex - transitionAnchorDisplayIndex),
                        seeking = transitionSeeking,
                    )
                    absolutePositions.getOrElse(currentDisplayIndex) { 0f } +
                            baseOffset + transitionOffset < anchorY
                }
            }
        }

        if (timingIndex.lines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                LyricsWaitingDots(
                    animated = false,
                    modifier = Modifier.size(width = 32.dp, height = 32.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .then(
                        if (userScrollEnabled) {
                            Modifier.scrollable(
                                state = manualScrollState,
                                orientation = Orientation.Vertical,
                            )
                        } else Modifier
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            val baseOffset = if (contentOffsetPx.isNaN()) {
                                targetContentOffsetPx
                            } else {
                                contentOffsetPx
                            }
                            IntOffset(x = 0, y = baseOffset.roundToInt())
                        },
                ) {
                    renderedItemRange.forEach { displayIndex ->
                        val displayItem = displayItems[displayIndex]
                        key(displayItem) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .layout { measurable, constraints ->
                                        val placeable = measurable.measure(
                                            constraints.copy(maxHeight = Constraints.Infinity)
                                        )
                                        layout(placeable.width, 0) {
                                            placeable.place(0, 0)
                                        }
                                    }
                                    .offset {
                                        val transitionOffset = fullLyricsTransitionOffset(
                                            startOffset = transitionStartOffsets
                                                .getOrElse(displayIndex) { 0f },
                                            elapsedMs = transitionElapsedMs,
                                            distanceFromAnchor = abs(
                                                displayIndex - transitionAnchorDisplayIndex
                                            ),
                                            seeking = transitionSeeking,
                                        )
                                        IntOffset(
                                            x = 0,
                                            y = (
                                                    absolutePositions[displayIndex] +
                                                            transitionOffset
                                                    ).roundToInt(),
                                        )
                                    },
                            ) {
                                when (displayItem) {
                                    is FullLyricsDisplayItem.Line -> {
                                        val lineIndex = displayItem.lineIndex
                                        val lineTiming = timingIndex.lines[lineIndex]
                                        val startsNewPositionGroup = lineIndex > 0 &&
                                                timingIndex.lines[lineIndex - 1].line.position !=
                                                lineTiming.line.position
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .lyricsItemEdgeFade(
                                                    itemTopPx = {
                                                        val baseOffset = if (contentOffsetPx.isNaN()) {
                                                            targetContentOffsetPx
                                                        } else {
                                                            contentOffsetPx
                                                        }
                                                        val transitionOffset = fullLyricsTransitionOffset(
                                                            startOffset = transitionStartOffsets
                                                                .getOrElse(displayIndex) { 0f },
                                                            elapsedMs = transitionElapsedMs,
                                                            distanceFromAnchor = abs(
                                                                displayIndex - transitionAnchorDisplayIndex
                                                            ),
                                                            seeking = transitionSeeking,
                                                        )
                                                        baseOffset + absolutePositions[displayIndex] +
                                                                transitionOffset
                                                    },
                                                    viewportHeightPx = { viewportHeightPx },
                                                    topFadePx = topFadePx,
                                                    bottomFadePx = bottomFadePx,
                                                )
                                                .onSizeChanged { size ->
                                                    if (itemHeights[displayIndex] != size.height) {
                                                        itemHeights[displayIndex] = size.height
                                                    }
                                                }
                                                .padding(
                                                    top = if (startsNewPositionGroup) 14.dp else 0.dp
                                                )
                                        ) {
                                            val onLineClick = {
                                                if (chromeCollapsed) {
                                                    onChromeInteraction()
                                                } else {
                                                    onChromeInteraction()
                                                    timelineState.positionMs =
                                                        lineTiming.line.startMs
                                                            .toFloat()
                                                            .coerceIn(0f, timelineState.durationMs)
                                                    if (autoScrollSuppressed) {
                                                        manualResyncNonce++
                                                    }
                                                }
                                            }
                                            when (lineTiming) {
                                                is WordLineTiming -> FullTimedLyricsLine(
                                                    timingIndex = timingIndex,
                                                    lineTiming = lineTiming,
                                                    timelineState = timelineState,
                                                    isActive = lineIndex == activeLineIndex,
                                                    isPast = lineIndex <= latestCompletedLineIndex,
                                                    isLatestCompleted =
                                                        lineIndex == latestCompletedLineIndex,
                                                    onClick = onLineClick,
                                                )

                                                is LineTiming -> FullLineLyricsLine(
                                                    lineTiming = lineTiming,
                                                    isActive = lineIndex == activeLineIndex,
                                                    isPast = lineIndex <= latestCompletedLineIndex,
                                                    onClick = onLineClick,
                                                )
                                            }
                                        }
                                    }

                                    is FullLyricsDisplayItem.Gap -> {
                                        val waitingGap = remember(displayItem) {
                                            LyricsWaitingGap(
                                                afterLineIndex = displayItem.afterLineIndex,
                                                startMs = displayItem.startMs,
                                                endMs = displayItem.endMs,
                                            )
                                        }
                                        FullLyricsTimelineGapIndicator(
                                            waitingGap = waitingGap,
                                            timelineState = timelineState,
                                            isPlaying = isPlaying,
                                            autoScrollSuppressed = autoScrollSuppressed,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .lyricsItemEdgeFade(
                                                    itemTopPx = {
                                                        val baseOffset = if (contentOffsetPx.isNaN()) {
                                                            targetContentOffsetPx
                                                        } else {
                                                            contentOffsetPx
                                                        }
                                                        val transitionOffset = fullLyricsTransitionOffset(
                                                            startOffset = transitionStartOffsets
                                                                .getOrElse(displayIndex) { 0f },
                                                            elapsedMs = transitionElapsedMs,
                                                            distanceFromAnchor = abs(
                                                                displayIndex - transitionAnchorDisplayIndex
                                                            ),
                                                            seeking = transitionSeeking,
                                                        )
                                                        baseOffset + absolutePositions[displayIndex] +
                                                                transitionOffset
                                                    },
                                                    viewportHeightPx = { viewportHeightPx },
                                                    topFadePx = topFadePx,
                                                    bottomFadePx = bottomFadePx,
                                                )
                                                .onSizeChanged { size ->
                                                    if (itemHeights[displayIndex] != size.height) {
                                                        itemHeights[displayIndex] = size.height
                                                    }
                                                },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val syncArrowRotation by animateFloatAsState(
            targetValue = if (syncArrowPointsUp) 180f else 0f,
            animationSpec = tween(220, easing = FastOutSlowInEasing),
        )

        AnimatedVisibility(
            visible = autoScrollSuppressed,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Button(
                onClick = {
                    directManualResyncActive = true
                    autoScrollSuppressed = false
                },
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 10.dp,
                    end = 8.dp,
                    bottom = 10.dp
                ),
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.onSecondaryContainer,
                    contentColor = colorScheme.secondaryContainer,
                ),
            ) {
                Text("Sync")
                Spacer(Modifier.width(4.dp))
                Icon(
                    painter = painterResource(Res.drawable.ic_keyboard_arrow_down),
                    contentDescription = if (syncArrowPointsUp) "Sync up" else "Sync down",
                    modifier = Modifier.graphicsLayer {
                        rotationZ = syncArrowRotation
                    },
                )
            }
        }
    }
}
