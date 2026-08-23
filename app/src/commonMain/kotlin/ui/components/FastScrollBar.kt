// TAKEN FROM https://github.com/tunjid/composables/
package dev.brahmkshatriya.echo.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withTimeout
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A [Scrollbar] that allows for fast scrolling of content.
 * Its thumb disappears when the scrolling container is dormant.
 * @param modifier a [Modifier] for the [Scrollbar]
 * @param state the driving state for the [Scrollbar]
 * @param scrollInProgress a flag indicating if the scrolling container for the scrollbar is
 * currently scrolling
 * @param orientation the orientation of the scrollbar
 * @param onThumbMoved the fast scroll implementation
 */
@Composable
fun FastScrollbar(
    modifier: Modifier = Modifier,
    state: ScrollbarState,
    scrollInProgress: Boolean,
    orientation: Orientation,
    onThumbMoved: (Float) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Scrollbar(
        modifier = modifier,
        orientation = orientation,
        interactionSource = interactionSource,
        state = state,
        thumb = {
            FastScrollbarThumb(
                scrollInProgress = scrollInProgress,
                interactionSource = interactionSource,
                orientation = orientation,
            )
        },
        onThumbMoved = onThumbMoved,
    )
}

/**
 * A scrollbar thumb that is intended to also be a touch target for fast scrolling.
 */
@Composable
private fun FastScrollbarThumb(
    scrollInProgress: Boolean,
    interactionSource: InteractionSource,
    orientation: Orientation,
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val active = pressed || hovered || dragged
    val thumbPadding by animateDpAsState(
        targetValue = if (active) 0.dp else 1.dp,
        animationSpec = tween(durationMillis = 60),
        label = "Scrollbar thumb padding",
    )
    val modifier = Modifier.run {
        when (orientation) {
            Vertical -> width(8.dp).fillMaxHeight()
            Horizontal -> height(8.dp).fillMaxWidth()
        }.padding(thumbPadding)
    }.background(
        color = scrollbarThumbColor(
            scrollInProgress = scrollInProgress,
            active = active,
        ),
        shape = RoundedCornerShape(4.dp),
    )
    Box(modifier)
}

/**
 * The color of the scrollbar thumb as a function of its interaction state.
 * @param scrollInProgress if the scrolling container is currently scrolling
 */
@Composable
private fun scrollbarThumbColor(
    scrollInProgress: Boolean,
    active: Boolean,
): Color {
    var dormant by remember { mutableStateOf(false) }
    LaunchedEffect(active, scrollInProgress) {
        dormant = false
        if (!active && !scrollInProgress) {
            delay(2.seconds)
            dormant = true
        }
    }

    val targetColor = when {
        active -> MaterialTheme.colorScheme.primary
        dormant -> Color.Transparent
        else -> MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.33f)
    }
    return animateColorAsState(
        targetValue = targetColor,
        animationSpec = SpringSpec(
            stiffness = Spring.StiffnessLow,
        ),
        label = "Scrollbar thumb color",
    ).value
}

/**
 * Linearly interpolates the index for the item at [index] in [visibleItems] to smoothly match the
 * scroll rate of the backing [ScrollableState].
 *
 * This method should not be read in composition as it changes frequently with scroll state.
 * Instead, it should be read in an in effect block inside a [snapshotFlow].
 *
 * @param visibleItems a list of items currently visible in the layout.
 * @param itemSize a lookup function for the size of an item in the layout.
 * @param offset a lookup function for the offset of an item relative to the start of the view port.
 * @param nextItemOnMainAxis a lookup function for the next item on the main axis in the direction
 * of the scroll.
 * @param itemIndex a lookup function for index of an item in the layout relative to
 * the total amount of items available.
 *
 * @return a [Float] in the range `[firstItemPosition..nextItemPosition)` or [Float.NaN] if:
 * - [visibleItems] returns an empty [List].
 * - [visibleItems] does not have an item at [index].
 * */
internal inline fun <LazyState : ScrollableState, LazyStateItem> interpolatedIndexOfVisibleItemAt(
    lazyState: LazyState,
    visibleItems: List<LazyStateItem>,
    index: Int,
    crossinline itemSize: LazyState.(LazyStateItem) -> Int,
    crossinline offset: LazyState.(LazyStateItem) -> Int,
    crossinline nextItemOnMainAxis: LazyState.(LazyStateItem) -> LazyStateItem?,
    crossinline itemIndex: (LazyStateItem) -> Int,
): Float {
    if (visibleItems.isEmpty()) return Float.NaN

    val item = visibleItems.getOrNull(index) ?: return Float.NaN
    val firstItemIndex = itemIndex(item)

    if (firstItemIndex < 0) return Float.NaN

    val firstItemSize = lazyState.itemSize(item)
    if (firstItemSize == 0) return Float.NaN

    val itemOffset = lazyState.offset(item).toFloat()
    val offsetPercentage = if (itemOffset > 0f) 0f else abs(itemOffset) / firstItemSize

    val nextItem = lazyState.nextItemOnMainAxis(item) ?: return firstItemIndex + offsetPercentage

    val nextItemIndex = itemIndex(nextItem)

    return firstItemIndex + ((nextItemIndex - firstItemIndex) * offsetPercentage)
}

/**
 * Returns the percentage of an item that is currently visible in the view port.
 * @param itemSize the size of the item
 * @param itemStartOffset the start offset of the item relative to the view port start
 * @param viewportStartOffset the start offset of the view port
 * @param viewportEndOffset the end offset of the view port
 */
internal fun itemVisibilityPercentage(
    itemSize: Int,
    itemStartOffset: Int,
    viewportStartOffset: Int,
    viewportEndOffset: Int,
): Float {
    if (itemSize == 0) return 0f
    val itemEnd = itemStartOffset + itemSize
    val startOffset = when {
        itemStartOffset > viewportStartOffset -> 0
        else -> abs(abs(viewportStartOffset) - abs(itemStartOffset))
    }
    val endOffset = when {
        itemEnd < viewportEndOffset -> 0
        else -> abs(abs(itemEnd) - abs(viewportEndOffset))
    }
    val size = itemSize.toFloat()
    return (size - startOffset - endOffset) / size
}

/**
 * Returns the value of [offset] along the axis specified by [this]
 */
internal fun Orientation.valueOf(offset: Offset) = when (this) {
    Horizontal -> offset.x
    Vertical -> offset.y
}

/**
 * Returns the value of [intSize] along the axis specified by [this]
 */
internal fun Orientation.valueOf(intSize: IntSize) = when (this) {
    Horizontal -> intSize.width
    Vertical -> intSize.height
}

@Composable
fun rememberScrollbarThumbMover(
    itemsAvailable: Int,
    itemSize: () -> Int,
    scroll: suspend (index: Int, scrollOffset: Int) -> Unit,
): (Float) -> Unit {
    var percentage by remember { mutableFloatStateOf(Float.NaN) }
    val itemCount by rememberUpdatedState(itemsAvailable)
    val currentItemSize by rememberUpdatedState(itemSize)
    val currentScroll by rememberUpdatedState(scroll)

    LaunchedEffect(percentage) {
        if (percentage.isNaN()) return@LaunchedEffect
        val itemPosition = scrollbarTargetPosition(itemCount, percentage)
        val itemSizePx = currentItemSize().coerceAtLeast(0)
        val scrollOffset = (itemPosition.offsetFraction * itemSizePx).roundToInt()
        currentScroll(itemPosition.index, scrollOffset)
    }
    return remember {
        { newPercentage -> percentage = newPercentage }
    }
}

private data class ScrollbarTargetPosition(
    val index: Int,
    val offsetFraction: Float,
)

private fun scrollbarTargetPosition(
    itemsAvailable: Int,
    percentage: Float,
): ScrollbarTargetPosition {
    if (itemsAvailable <= 0) return ScrollbarTargetPosition(index = 0, offsetFraction = 0f)

    val maxPosition = (itemsAvailable - 1).toFloat()
    val targetPosition = (itemsAvailable * percentage).coerceIn(0f, maxPosition)
    val index = targetPosition.toInt()
    return ScrollbarTargetPosition(
        index = index,
        offsetFraction = targetPosition - index,
    )
}

inline fun <T> List<T>.sumOf(selector: (T) -> Float): Float =
    fold(initial = 0f) { accumulator, listItem -> accumulator + selector(listItem) }

/**
 * Calculates a [ScrollbarState] driven by the changes in a [LazyListState].
 *
 * The calculations for [ScrollbarState] assumes homogeneous items. For heterogeneous items,
 * the produced state may not change smoothly. If this is the case, you may derive your own
 * [ScrollbarState] using an algorithm that better fits your list items.
 *
 * @param itemsAvailable the total amount of items available to scroll in the lazy list.
 * @param itemIndex a lookup function for index of an item in the list relative to [itemsAvailable].
 */
@Composable
fun LazyListState.scrollbarState(
    itemsAvailable: Int,
    itemIndex: (LazyListItemInfo) -> Int = LazyListItemInfo::index,
    isScrollbarItem: (LazyListItemInfo) -> Boolean = { true },
    itemSize: ((LazyListItemInfo) -> Int)? = null,
): ScrollbarState {
    val state = remember { ScrollbarState() }
    val currentItemIndex by rememberUpdatedState(itemIndex)
    val currentIsScrollbarItem by rememberUpdatedState(isScrollbarItem)
    val currentItemSize by rememberUpdatedState(itemSize)
    LaunchedEffect(this, itemsAvailable) {
        snapshotFlow {
            if (itemsAvailable == 0) return@snapshotFlow null

            val visibleItemsInfo = layoutInfo.visibleItemsInfo.filter(currentIsScrollbarItem)
            if (visibleItemsInfo.isEmpty()) return@snapshotFlow null
            val estimatedItemSize = currentItemSize?.let { sizeOf ->
                visibleItemsInfo.firstNotNullOfOrNull { itemInfo ->
                    sizeOf(itemInfo).takeIf { it > 0 }
                }
            }

            val firstIndex = min(
                a = interpolatedIndexOfVisibleItemAt(
                    lazyState = this@scrollbarState,
                    visibleItems = visibleItemsInfo,
                    index = 0,
                    itemSize = { it.size },
                    offset = { it.offset },
                    nextItemOnMainAxis = { item ->
                        visibleItemsInfo.getOrNull(visibleItemsInfo.indexOf(item) + 1)
                    },
                    itemIndex = currentItemIndex,
                ),
                b = itemsAvailable.toFloat(),
            )
            if (firstIndex.isNaN()) return@snapshotFlow null

            val itemsVisible = visibleItemsInfo.sumOf { itemInfo ->
                itemVisibilityPercentage(
                    itemSize = itemInfo.size,
                    itemStartOffset = itemInfo.offset,
                    viewportStartOffset = layoutInfo.viewportStartOffset,
                    viewportEndOffset = layoutInfo.viewportEndOffset,
                )
            }
            val viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset

            val thumbTravelPercent = min(
                a = firstIndex / itemsAvailable,
                b = 1f,
            )
            val thumbSizePercent = min(
                a = if (estimatedItemSize != null)
                    viewportSize.toFloat() / (estimatedItemSize * itemsAvailable)
                else itemsVisible / itemsAvailable,
                b = 1f,
            )
            scrollbarStateValue(
                thumbSizePercent = thumbSizePercent,
                thumbMovedPercent = when {
                    layoutInfo.reverseLayout -> 1f - thumbTravelPercent
                    else -> thumbTravelPercent
                },
            )
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { state.onScroll(it) }
    }
    return state
}


/**
 * The delay between scrolls when a user long presses on the scrollbar track to initiate a scroll
 * instead of dragging the scrollbar thumb.
 */
private const val SCROLLBAR_PRESS_DELAY_MS = 10L

/**
 * The percentage displacement of the scrollbar when scrolled by long presses on the scrollbar
 * track.
 */
private const val SCROLLBAR_PRESS_DELTA_PCT = 0.02f

@Stable
class ScrollbarState {
    private var packedValue by mutableLongStateOf(0L)

    internal fun onScroll(stateValue: ScrollbarStateValue) {
        packedValue = stateValue.packedValue
    }

    /**
     * Returns the thumb size of the scrollbar as a percentage of the total track size
     */
    val thumbSizePercent
        get() = unpackFloat1(packedValue)

    /**
     * Returns the distance the thumb has traveled as a percentage of total track size
     */
    val thumbMovedPercent
        get() = unpackFloat2(packedValue)

    /**
     * Returns the max distance the thumb can travel as a percentage of total track size
     */
    val thumbTrackSizePercent
        get() = 1f - thumbSizePercent
}

/**
 * Returns the size of the scrollbar track in pixels
 */
private val ScrollbarTrack.size
    get() = unpackFloat2(packedValue) - unpackFloat1(packedValue)

/**
 * Returns the position of the scrollbar thumb on the track as a percentage
 */
private fun ScrollbarTrack.thumbPosition(
    dimension: Float,
): Float = max(
    a = min(
        a = dimension / size,
        b = 1f,
    ),
    b = 0f,
)

/**
 * Class definition for the core properties of a scroll bar
 */
@Immutable
@ConsistentCopyVisibility
data class ScrollbarStateValue internal constructor(
    internal val packedValue: Long,
)

/**
 * Class definition for the core properties of a scroll bar track
 */
@Immutable
private data class ScrollbarTrack(
    val packedValue: Long,
) {
    constructor(
        max: Float,
        min: Float,
    ) : this(packFloats(max, min))
}

/**
 * Creates a [ScrollbarStateValue] with the listed properties
 * @param thumbSizePercent the thumb size of the scrollbar as a percentage of the total track size.
 *  Refers to either the thumb width (for horizontal scrollbars)
 *  or height (for vertical scrollbars).
 * @param thumbMovedPercent the distance the thumb has traveled as a percentage of total
 * track size.
 */
fun scrollbarStateValue(
    thumbSizePercent: Float,
    thumbMovedPercent: Float,
) = ScrollbarStateValue(
    packFloats(
        val1 = thumbSizePercent,
        val2 = thumbMovedPercent,
    ),
)

/**
 * A Composable for drawing a scrollbar
 * @param orientation the scroll direction of the scrollbar
 * @param state the state describing the position of the scrollbar
 * @param minThumbSize the minimum size of the scrollbar thumb
 * @param interactionSource allows for observing the state of the scroll bar
 * @param thumb a composable for drawing the scrollbar thumb
 * @param onThumbMoved a function for reacting to scroll bar displacements caused by direct
 * interactions on the scrollbar thumb by the user, for example implementing a fast scroll
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun Scrollbar(
    modifier: Modifier = Modifier,
    orientation: Orientation,
    state: ScrollbarState,
    minThumbSize: Dp = 48.dp,
    interactionSource: MutableInteractionSource? = null,
    thumb: @Composable () -> Unit,
    onThumbMoved: ((Float) -> Unit)? = null,
) {
    // Using Offset.Unspecified and Float.NaN instead of null
    // to prevent unnecessary boxing of primitives
    var pressedOffset by remember { mutableStateOf(Offset.Unspecified) }
    var dragInProgress by remember { mutableStateOf(false) }

    // Used to immediately show drag feedback in the UI while the scrolling implementation
    // catches up
    var interactionThumbTravelPercent by remember { mutableFloatStateOf(Float.NaN) }

    var track by remember { mutableStateOf(ScrollbarTrack(packedValue = 0)) }

    // scrollbar track container
    Box(
        modifier = modifier
            .run {
                val withHover = interactionSource?.let(::hoverable) ?: this
                when (orientation) {
                    Vertical -> withHover.fillMaxHeight()
                    Horizontal -> withHover.fillMaxWidth()
                }
            }
            .onPlaced { coordinates ->
                val scrollbarStartCoordinate = orientation.valueOf(coordinates.positionInRoot())
                track = ScrollbarTrack(
                    max = scrollbarStartCoordinate,
                    min = scrollbarStartCoordinate + orientation.valueOf(coordinates.size),
                )
            }
            // Process scrollbar presses
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        try {
                            // Wait for a long press before scrolling
                            withTimeout(viewConfiguration.longPressTimeoutMillis.milliseconds) {
                                tryAwaitRelease()
                            }
                        } catch (_: TimeoutCancellationException) {
                            // Start the press triggered scroll
                            val initialPress = PressInteraction.Press(offset)
                            interactionSource?.tryEmit(initialPress)

                            pressedOffset = offset
                            interactionSource?.tryEmit(
                                when {
                                    tryAwaitRelease() -> PressInteraction.Release(initialPress)
                                    else -> PressInteraction.Cancel(initialPress)
                                },
                            )

                            // End the press
                            pressedOffset = Offset.Unspecified
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                var dragInteraction: DragInteraction.Start? = null
                var dragStartTravelPercent = Float.NaN
                var dragStartPointerPositionPx = Float.NaN

                val finishDrag: (Boolean) -> Unit = { cancelled ->
                    dragInteraction?.let { start ->
                        interactionSource?.tryEmit(
                            if (cancelled) DragInteraction.Cancel(start) else DragInteraction.Stop(
                                start
                            )
                        )
                    }
                    dragInteraction = null
                    dragStartTravelPercent = Float.NaN
                    dragStartPointerPositionPx = Float.NaN
                    dragInProgress = false
                    interactionThumbTravelPercent = Float.NaN
                }
                val onDragStart: (Offset) -> Unit = onDragStart@{ downPosition ->
                    val thumbSizePx = max(
                        a = state.thumbSizePercent * track.size,
                        b = minThumbSize.toPx(),
                    )
                    val trackSizePx = when (state.thumbTrackSizePercent) {
                        0f -> track.size
                        else -> (track.size - thumbSizePx) / state.thumbTrackSizePercent
                    }
                    val thumbTravelPercent = when {
                        interactionThumbTravelPercent.isNaN() -> state.thumbMovedPercent
                        else -> interactionThumbTravelPercent
                    }.coerceIn(0f, state.thumbTrackSizePercent)
                    val thumbStartPx = trackSizePx * thumbTravelPercent
                    val downPositionPx = orientation.valueOf(downPosition)
                    if (downPositionPx !in thumbStartPx..thumbStartPx + thumbSizePx) {
                        return@onDragStart
                    }

                    val start = DragInteraction.Start()
                    dragInteraction = start
                    interactionSource?.tryEmit(start)
                    dragStartTravelPercent = when {
                        interactionThumbTravelPercent.isNaN() -> state.thumbMovedPercent
                        else -> interactionThumbTravelPercent
                    }
                    dragInProgress = true
                    interactionThumbTravelPercent = dragStartTravelPercent
                    pressedOffset = Offset.Unspecified
                    dragStartPointerPositionPx = downPositionPx
                }
                val onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit =
                    onDrag@{ change, _ ->
                        if (dragStartTravelPercent.isNaN() || dragStartPointerPositionPx.isNaN()) {
                            return@onDrag
                        }

                        val pointerDisplacementPx = orientation.valueOf(change.position) -
                                dragStartPointerPositionPx
                        val thumbSizePx = max(
                            a = state.thumbSizePercent * track.size,
                            b = minThumbSize.toPx(),
                        )
                        val maxThumbTravelPx = (track.size - thumbSizePx).coerceAtLeast(0f)
                        val deltaTravelPercent = when {
                            maxThumbTravelPx <= 0f -> 0f
                            else -> pointerDisplacementPx / maxThumbTravelPx *
                                    state.thumbTrackSizePercent
                        }
                        val currentTravel = (dragStartTravelPercent + deltaTravelPercent)
                            .coerceIn(0f, state.thumbTrackSizePercent)
                        interactionThumbTravelPercent = currentTravel
                        onThumbMoved?.invoke(currentTravel)
                    }

                detectDragGestures(
                    orientationLock = orientation,
                    onDragStart = { down, _, _ -> onDragStart(down.position) },
                    onDragEnd = { finishDrag(false) },
                    onDragCancel = { finishDrag(true) },
                    onDrag = onDrag,
                )
            },
    ) {
        // scrollbar thumb container
        Layout(content = { thumb() }) { measurables, constraints ->
            val measurable = measurables.first()

            val thumbSizePx = max(
                a = state.thumbSizePercent * track.size,
                b = minThumbSize.toPx(),
            )

            val trackSizePx = when (state.thumbTrackSizePercent) {
                0f -> track.size
                else -> (track.size - thumbSizePx) / state.thumbTrackSizePercent
            }

            val thumbTravelPercent = max(
                a = min(
                    a = when {
                        interactionThumbTravelPercent.isNaN() -> state.thumbMovedPercent
                        else -> interactionThumbTravelPercent
                    },
                    b = state.thumbTrackSizePercent,
                ),
                b = 0f,
            )

            val thumbMovedPx = trackSizePx * thumbTravelPercent

            val y = when (orientation) {
                Horizontal -> 0
                Vertical -> thumbMovedPx.roundToInt()
            }
            val x = when (orientation) {
                Horizontal -> thumbMovedPx.roundToInt()
                Vertical -> 0
            }

            val updatedConstraints = when (orientation) {
                Horizontal -> {
                    constraints.copy(
                        minWidth = thumbSizePx.roundToInt(),
                        maxWidth = thumbSizePx.roundToInt(),
                    )
                }

                Vertical -> {
                    constraints.copy(
                        minHeight = thumbSizePx.roundToInt(),
                        maxHeight = thumbSizePx.roundToInt(),
                    )
                }
            }

            val placeable = measurable.measure(updatedConstraints)
            layout(placeable.width, placeable.height) {
                placeable.place(x, y)
            }
        }
    }

    if (onThumbMoved == null) return
    LaunchedEffect(Unit) {
        snapshotFlow { pressedOffset }.collectLatest { pressedOffset ->
            if (pressedOffset == Offset.Unspecified) {
                if (!dragInProgress) interactionThumbTravelPercent = Float.NaN
                return@collectLatest
            }

            var currentThumbMovedPercent = state.thumbMovedPercent
            val destinationThumbMovedPercent = track.thumbPosition(
                dimension = orientation.valueOf(pressedOffset),
            )
            val isPositive = currentThumbMovedPercent < destinationThumbMovedPercent
            val delta = SCROLLBAR_PRESS_DELTA_PCT * if (isPositive) 1f else -1f

            while (currentThumbMovedPercent != destinationThumbMovedPercent) {
                currentThumbMovedPercent = when {
                    isPositive -> min(
                        a = currentThumbMovedPercent + delta,
                        b = destinationThumbMovedPercent,
                    )

                    else -> max(
                        a = currentThumbMovedPercent + delta,
                        b = destinationThumbMovedPercent,
                    )
                }
                onThumbMoved(currentThumbMovedPercent)
                interactionThumbTravelPercent = currentThumbMovedPercent
                delay(SCROLLBAR_PRESS_DELAY_MS.milliseconds)
            }
        }
    }
}