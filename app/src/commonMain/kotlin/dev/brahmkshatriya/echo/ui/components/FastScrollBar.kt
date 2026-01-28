@file:Suppress("AssignedValueIsNeverRead", "COMPOSE_APPLIER_CALL_MISMATCH")
// TOTALLY TAKEN FROM https://github.com/tunjid/composables/

package dev.brahmkshatriya.echo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withTimeout
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
    Box(
        modifier = Modifier
            .run {
                when (orientation) {
                    Vertical -> width(8.dp).fillMaxHeight()
                    Horizontal -> height(8.dp).fillMaxWidth()
                }
            }
            .background(
                color = scrollbarThumbColor(
                    scrollInProgress = scrollInProgress,
                    interactionSource = interactionSource,
                ),
                shape = RoundedCornerShape(8.dp),
            ),
    )
}

/**
 * The color of the scrollbar thumb as a function of its interaction state.
 * @param scrollInProgress if the scrolling container is currently scrolling
 * @param interactionSource source of interactions in the scrolling container
 */
@Composable
private fun scrollbarThumbColor(
    scrollInProgress: Boolean,
    interactionSource: InteractionSource,
): Color {
    var state by remember { mutableStateOf(ThumbState.Active) }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val active = pressed || hovered || dragged

    val color by animateColorAsState(
        targetValue = when (state) {
            ThumbState.Active -> MaterialTheme.colorScheme.primary
            ThumbState.Inactive -> MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.33f)
            ThumbState.Dormant -> Color.Transparent
        },
        animationSpec = SpringSpec(
            stiffness = Spring.StiffnessLow,
        ),
        label = "Scrollbar thumb color",
    )
    LaunchedEffect(active, scrollInProgress) {
        when {
            active -> state = ThumbState.Active
            scrollInProgress -> state = ThumbState.Inactive
            else -> {
                state = ThumbState.Inactive
                delay(2_000)
                state = ThumbState.Dormant
            }
        }
    }

    return color
}

private enum class ThumbState {
    Active, Inactive, Dormant
}
/**
 * Linearly interpolates the index for the item at [index] in [LazyListLayoutInfo.visibleItemsInfo]
 * to smoothly match the scroll rate of this [LazyListState].
 *
 * This method should not be read in composition as it changes frequently with scroll state.
 * Instead it should be read in an in effect block inside of a [snapshotFlow].
 *
 * @param index the index for which its interpolated index in [LazyListLayoutInfo.visibleItemsInfo]
 * should be returned.
 *
 * @param itemIndex a look up for the index for the item in [LazyListLayoutInfo.visibleItemsInfo].
 * It defaults to [LazyListItemInfo.index].
 *
 * @return a [Float] in the range [firstItemPosition..nextItemPosition)
 * in [LazyListLayoutInfo.visibleItemsInfo] or [Float.NaN] if:
 * - [LazyListLayoutInfo.visibleItemsInfo] is empty.
 * - [LazyListLayoutInfo.visibleItemsInfo] does not have an item at [index].
 * */
fun LazyListState.interpolatedIndexOfVisibleItemAt(
    index: Int,
    itemIndex: (LazyListItemInfo) -> Int = LazyListItemInfo::index,
): Float {
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    return interpolatedIndexOfVisibleItemAt(
        lazyState = this,
        visibleItems = visibleItemsInfo,
        index = index,
        itemSize = { it.size },
        offset = { it.offset },
        nextItemOnMainAxis = { visibleItemsInfo.getOrNull(index + 1) },
        itemIndex = itemIndex,
    )
}

/**
 * Linearly interpolates the index for the first item in [LazyListLayoutInfo.visibleItemsInfo]
 * to smoothly match the scroll rate of this [LazyListState].
 *
 * This method should not be read in composition as it changes frequently with scroll state.
 * Instead it should be read in an in effect block inside of a [snapshotFlow].
 *
 * @param itemIndex a look up for the index for the item in [LazyListLayoutInfo.visibleItemsInfo].
 * It defaults to [LazyListItemInfo.index].
 *
 * @see [LazyListState.interpolatedIndexOfVisibleItemAt]
 *
 * @return a [Float] in the range [firstItemPosition..nextItemPosition)
 * in [LazyListLayoutInfo.visibleItemsInfo] or [Float.NaN] if:
 * - [LazyListLayoutInfo.visibleItemsInfo] is empty.
 * - [LazyListLayoutInfo.visibleItemsInfo] does not have an item at the first visible index.
 * */
fun LazyListState.interpolatedFirstItemIndex(
    itemIndex: (LazyListItemInfo) -> Int = LazyListItemInfo::index,
): Float = interpolatedIndexOfVisibleItemAt(
    index = 0,
    itemIndex = itemIndex,
)


/**
 * Linearly interpolates the index for the item at [index] in [visibleItems] to smoothly match the
 * scroll rate of the backing [ScrollableState].
 *
 * This method should not be read in composition as it changes frequently with scroll state.
 * Instead it should be read in an in effect block inside of a [snapshotFlow].
 *
 * @param visibleItems a list of items currently visible in the layout.
 * @param itemSize a lookup function for the size of an item in the layout.
 * @param offset a lookup function for the offset of an item relative to the start of the view port.
 * @param nextItemOnMainAxis a lookup function for the next item on the main axis in the direction
 * of the scroll.
 * @param itemIndex a lookup function for index of an item in the layout relative to
 * the total amount of items available.
 *
 * @return a [Float] in the range [firstItemPosition..nextItemPosition) or [Float.NaN] if:
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
    val offsetPercentage = abs(itemOffset) / firstItemSize

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

/**
 * Remembers a function to react to [Scrollbar] thumb position displacements for a
 * [ScrollState]
 */
@Composable
fun ScrollState.rememberScrollbarThumbMover(): (Float) -> Unit {
    var percentage by remember { mutableFloatStateOf(Float.NaN) }

    LaunchedEffect(percentage) {
        if (percentage.isNaN()) return@LaunchedEffect
        scrollTo((maxValue * percentage).roundToInt())
    }
    return remember {
        { newPercentage -> percentage = newPercentage }
    }
}

/**
 * Generic function to react to scrollbar thumb movements for a [ScrollbarState].
 * @param itemsAvailable the total amount of items available to scroll in the layout.
 * @param scroll a function to be invoked when an index has been identified to scroll to.
 */
@Composable
inline fun rememberScrollbarThumbMover(
    itemsAvailable: Int,
    crossinline scroll: suspend (index: Int) -> Unit,
): (Float) -> Unit {
    var percentage by remember { mutableFloatStateOf(Float.NaN) }
    val itemCount by rememberUpdatedState(itemsAvailable)

    LaunchedEffect(percentage) {
        if (percentage.isNaN()) return@LaunchedEffect
        val indexToFind = (itemCount * percentage).roundToInt()
        scroll(indexToFind)
    }
    return remember {
        { newPercentage -> percentage = newPercentage }
    }
}

inline fun <T> List<T>.sumOf(selector: (T) -> Float): Float =
    fold(initial = 0f) { accumulator, listItem -> accumulator + selector(listItem) }

/**
 * Remembers a [ScrollbarState] driven by the changes in a [ScrollbarState].
 */
@Composable
fun ScrollState.scrollbarState(): ScrollbarState {
    val state = remember { ScrollbarState() }
    LaunchedEffect(this) {
        snapshotFlow {
            scrollbarStateValue(
                thumbSizePercent = viewportSize.toFloat() / maxValue,
                thumbMovedPercent = value.toFloat() / maxValue,
            )
        }
            .collect { state.onScroll(it) }
    }
    return state
}

/**
 * Remembers a function to react to [Scrollbar] thumb position displacements for a [LazyListState]
 * based on the total items in the list, and [LazyListState.scrollToItem] for responding to
 * scrollbar thumb displacements.
 *
 * For more customization, including animated scrolling @see [rememberScrollbarThumbMover].
 */
@Composable
fun LazyListState.rememberBasicScrollbarThumbMover(): (Float) -> Unit {
    var totalItemsCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(this) {
        snapshotFlow { layoutInfo.totalItemsCount }
            .collect { totalItemsCount = it }
    }
    return rememberScrollbarThumbMover(
        itemsAvailable = totalItemsCount,
        scroll = ::scrollToItem,
    )
}

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
): ScrollbarState {
    val state = remember { ScrollbarState() }
    LaunchedEffect(this, itemsAvailable) {
        snapshotFlow {
            if (itemsAvailable == 0) return@snapshotFlow null

            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@snapshotFlow null

            val firstIndex = min(
                a = interpolatedFirstItemIndex(itemIndex),
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

            val thumbTravelPercent = min(
                a = firstIndex / itemsAvailable,
                b = 1f,
            )
            val thumbSizePercent = min(
                a = itemsVisible / itemsAvailable,
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
@JvmInline
value class ScrollbarStateValue internal constructor(
    internal val packedValue: Long,
)

/**
 * Class definition for the core properties of a scroll bar track
 */
@Immutable
@JvmInline
private value class ScrollbarTrack(
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
 * @param onThumbMoved an function for reacting to scroll bar displacements caused by direct
 * interactions on the scrollbar thumb by the user, for example implementing a fast scroll
 */
@Composable
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
    var draggedOffset by remember { mutableStateOf(Offset.Unspecified) }

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
                            withTimeout(viewConfiguration.longPressTimeoutMillis) {
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
            // Process scrollbar drags
            .pointerInput(Unit) {
                var dragInteraction: DragInteraction.Start? = null
                val onDragStart: (Offset) -> Unit = { offset ->
                    val start = DragInteraction.Start()
                    dragInteraction = start
                    interactionSource?.tryEmit(start)
                    draggedOffset = offset
                }
                val onDragEnd: () -> Unit = {
                    dragInteraction?.let { interactionSource?.tryEmit(DragInteraction.Stop(it)) }
                    draggedOffset = Offset.Unspecified
                }
                val onDragCancel: () -> Unit = {
                    dragInteraction?.let { interactionSource?.tryEmit(DragInteraction.Cancel(it)) }
                    draggedOffset = Offset.Unspecified
                }
                val onDrag: (change: PointerInputChange, dragAmount: Float) -> Unit =
                    onDrag@{ _, delta ->
                        if (draggedOffset == Offset.Unspecified) return@onDrag
                        draggedOffset = when (orientation) {
                            Vertical -> draggedOffset.copy(
                                y = draggedOffset.y + delta,
                            )

                            Horizontal -> draggedOffset.copy(
                                x = draggedOffset.x + delta,
                            )
                        }
                    }

                when (orientation) {
                    Horizontal -> detectHorizontalDragGestures(
                        onDragStart = onDragStart,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                        onHorizontalDrag = onDrag,
                    )

                    Vertical -> detectVerticalDragGestures(
                        onDragStart = onDragStart,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                        onVerticalDrag = onDrag,
                    )
                }
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

    // Process presses
    LaunchedEffect(Unit) {
        snapshotFlow { pressedOffset }.collect { pressedOffset ->
            // Press ended, reset interactionThumbTravelPercent
            if (pressedOffset == Offset.Unspecified) {
                interactionThumbTravelPercent = Float.NaN
                return@collect
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
                delay(SCROLLBAR_PRESS_DELAY_MS)
            }
        }
    }

    // Process drags
    LaunchedEffect(Unit) {
        snapshotFlow { draggedOffset }.collect { draggedOffset ->
            if (draggedOffset == Offset.Unspecified) {
                interactionThumbTravelPercent = Float.NaN
                return@collect
            }
            val currentTravel = track.thumbPosition(
                dimension = orientation.valueOf(draggedOffset),
            )
            onThumbMoved(currentTravel)
            interactionThumbTravelPercent = currentTravel
        }
    }
}