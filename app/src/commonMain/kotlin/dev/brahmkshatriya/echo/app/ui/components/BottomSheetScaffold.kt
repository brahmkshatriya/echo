package dev.brahmkshatriya.echo.app.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalPointerSlopOrCancellation
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull
import dev.brahmkshatriya.echo.app.platform.onPointerScrollY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

private const val BottomSheetDragHandleDescription = "Drag handle"
private const val BottomSheetPartialExpandDescription = "Collapse"
private const val BottomSheetDismissDescription = "Dismiss"
private const val BottomSheetExpandDescription = "Expand"

@Stable
@ExperimentalMaterial3Api
class SheetState
internal constructor(
    internal val enabledValues: Set<SheetValue>,
    internal val positionalThreshold: () -> Float,
    internal val velocityThreshold: () -> Float,
    initialValue: SheetValue,
    internal val confirmValueChange: (SheetValue) -> Boolean,
    internal val isBottomSheetPartiallyExpandedDeterministicEnabled: Boolean,
) {

    internal val skipPartiallyExpanded: Boolean
        get() = !enabledValues.contains(SheetValue.PartiallyExpanded)

    internal val skipHiddenState: Boolean
        get() = !enabledValues.contains(SheetValue.Hidden)

    init {
        require(enabledValues.contains(SheetValue.Expanded)) {
            "Expanded must be one of the enabled values."
        }
        require(enabledValues.contains(initialValue)) {
            "The initial value must be one of the enabled values."
        }
    }

    val currentValue: SheetValue
        get() = anchoredDraggableState.settledValue

    val targetValue: SheetValue by derivedStateOf {
        if (isAnimationRunning) {
            anchoredDraggableState.targetValue
        } else {
            calculateTargetValueWithFix(offset)
        }
    }

    private fun calculateTargetValueWithFix(currentOffset: Float): SheetValue {
        return if (!currentOffset.isNaN()) {
            val currentValueOffset = anchoredDraggableState.anchors.positionOf(currentValue)
            if (currentValueOffset.isNaN() || currentOffset == currentValueOffset) {
                currentValue
            } else {
                anchoredDraggableState.anchors.closestAnchor(currentOffset) ?: currentValue
            }
        } else currentValue
    }

    val isVisible: Boolean
        get() = anchoredDraggableState.currentValue != SheetValue.Hidden

    val isAnimationRunning: Boolean
        get() = anchoredDraggableState.isAnimationRunning

    fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    val hasExpandedState: Boolean
        get() = anchoredDraggableState.anchors.hasPositionFor(SheetValue.Expanded)

    val hasPartiallyExpandedState: Boolean
        get() = anchoredDraggableState.anchors.hasPositionFor(SheetValue.PartiallyExpanded)

    suspend fun expand() {
        if (confirmValueChange(SheetValue.Expanded)) {
            animateTo(SheetValue.Expanded, showMotionSpec)
        }
    }

    suspend fun partialExpand() {
        check(!skipPartiallyExpanded) {
            "Attempted to animate to partial expanded when skipPartiallyExpanded was enabled. Set skipPartiallyExpanded to false to use this function."
        }
        if (confirmValueChange(SheetValue.PartiallyExpanded)) {
            animateTo(SheetValue.PartiallyExpanded, hideMotionSpec)
        }
    }

    suspend fun show() {
        val targetValue = when {
            hasPartiallyExpandedState -> SheetValue.PartiallyExpanded
            else -> SheetValue.Expanded
        }
        if (confirmValueChange(targetValue)) {
            animateTo(targetValue, showMotionSpec)
        }
    }

    suspend fun hide() {
        check(!skipHiddenState) {
            "Attempted to animate to hidden when skipHiddenState was enabled. Set skipHiddenState to false to use this function."
        }
        if (confirmValueChange(SheetValue.Hidden)) {
            animateTo(SheetValue.Hidden, hideMotionSpec)
        }
    }

    internal suspend fun animateTo(
        targetValue: SheetValue,
        animationSpec: FiniteAnimationSpec<Float>,
    ) = anchoredDraggableState.animateTo(targetValue, animationSpec)

    internal suspend fun snapTo(targetValue: SheetValue) {
        anchoredDraggableState.snapTo(targetValue)
    }

    internal var anchoredDraggableMotionSpec: AnimationSpec<Float> = BottomSheetAnimationSpec

    internal var anchoredDraggableState: AnchoredDraggableState<SheetValue> =
        AnchoredDraggableState(initialValue = initialValue)

    internal fun newOffsetForDelta(delta: Float) =
        ((if (offset.isNaN()) 0f else offset) + delta).coerceIn(
            anchoredDraggableState.anchors.minPosition(),
            anchoredDraggableState.anchors.maxPosition(),
        )

    internal suspend fun anchoredDrag(flingBehavior: FlingBehavior, initialVelocity: Float): Float {
        var consumedVelocity = 0f
        anchoredDraggableState.anchoredDrag {
            val scrollScope =
                object : ScrollScope {
                    override fun scrollBy(pixels: Float): Float {
                        val newOffset = newOffsetForDelta(pixels)
                        val consumed = newOffset - offset
                        dragTo(newOffset)
                        return consumed
                    }
                }
            consumedVelocity = with(flingBehavior) { scrollScope.performFling(initialVelocity) }
        }
        return consumedVelocity
    }

    internal val offset: Float
        get() = anchoredDraggableState.offset

    internal var showMotionSpec: FiniteAnimationSpec<Float> = snap()

    internal var hideMotionSpec: FiniteAnimationSpec<Float> = snap()

    companion object {

        internal fun Saver(
            enabledValues: Set<SheetValue>,
            positionalThreshold: () -> Float,
            velocityThreshold: () -> Float,
            confirmValueChange: (SheetValue) -> Boolean,
            isBottomSheetPartiallyExpandedDeterministicEnabled: Boolean,
        ): Saver<SheetState, SheetValue> =
            Saver(
                save = { it.currentValue },
                restore = { savedValue ->
                    SheetState(
                        enabledValues = enabledValues,
                        positionalThreshold = positionalThreshold,
                        velocityThreshold = velocityThreshold,
                        initialValue = savedValue,
                        confirmValueChange = confirmValueChange,
                        isBottomSheetPartiallyExpandedDeterministicEnabled =
                            isBottomSheetPartiallyExpandedDeterministicEnabled,
                    )
                },
            )

    }
}

@Composable
@ExperimentalMaterial3Api
fun rememberBottomSheetState(
    initialValue: SheetValue,
    enabledValues: Set<SheetValue> = setOf(
        SheetValue.Hidden,
        SheetValue.PartiallyExpanded,
        SheetValue.Expanded
    ),
    confirmValueChange: (SheetValue) -> Boolean = { true },
): SheetState =
    rememberSheetState(
        enabledValues = enabledValues,
        confirmValueChange = confirmValueChange,
        initialValue = initialValue,
    )

@Composable
@ExperimentalMaterial3Api
fun rememberStandardBottomSheetState(
    initialValue: SheetValue = SheetValue.PartiallyExpanded,
    confirmValueChange: (SheetValue) -> Boolean = { true },
    skipHiddenState: Boolean = true,
) =
    rememberSheetState(
        initialValue = initialValue,
        enabledValues =
            if (skipHiddenState) setOf(SheetValue.PartiallyExpanded, SheetValue.Expanded)
            else setOf(SheetValue.Hidden, SheetValue.PartiallyExpanded, SheetValue.Expanded),
        confirmValueChange = confirmValueChange,
        isBottomSheetPartiallyExpandedDeterministicEnabled = false,
    )

@Stable
@ExperimentalMaterial3Api
class BottomSheetScaffoldState(
    val bottomSheetState: SheetState,
    val snackbarHostState: SnackbarHostState,
)

@Composable
@ExperimentalMaterial3Api
fun rememberBottomSheetScaffoldState(
    bottomSheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        enabledValues = setOf(SheetValue.PartiallyExpanded, SheetValue.Expanded),
    ),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
): BottomSheetScaffoldState {
    return remember(bottomSheetState, snackbarHostState) {
        BottomSheetScaffoldState(
            bottomSheetState = bottomSheetState,
            snackbarHostState = snackbarHostState,
        )
    }
}

@Stable
@ExperimentalMaterial3Api
object BottomSheetDefaults {

    val ExpandedShape: Shape
        @Composable
        get() =
            RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp,
                bottomEnd = 0.dp,
                bottomStart = 0.dp,
            )

    val ContainerColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow

    val Elevation: Dp = 1.dp

    val SheetPeekHeight = 56.dp

    val SheetMaxWidth = 640.dp

    internal val PositionalThreshold = 56.dp

    internal val VelocityThreshold = 125.dp

    @Composable
    fun DragHandle(
        modifier: Modifier = Modifier,
        width: Dp = 32.dp,
        height: Dp = 4.dp,
        shape: Shape = MaterialTheme.shapes.extraLarge,
        color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Surface(
            modifier =
                modifier
                    .padding(vertical = DragHandleVerticalPadding)
                    .semantics { contentDescription = BottomSheetDragHandleDescription },
            color = color,
            shape = shape,
        ) {
            Box(Modifier.size(width = width, height = height))
        }
    }
}

@Composable
@ExperimentalMaterial3Api
fun BottomSheetScaffold(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    sheetPeekHeight: Dp = BottomSheetDefaults.SheetPeekHeight,
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetShape: Shape = BottomSheetDefaults.ExpandedShape,
    sheetContainerColor: Color = BottomSheetDefaults.ContainerColor,
    sheetContentColor: Color = contentColorFor(sheetContainerColor),
    sheetTonalElevation: Dp = 0.dp,
    sheetShadowElevation: Dp = BottomSheetDefaults.Elevation,
    sheetDragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    sheetSwipeEnabled: Boolean = true,
    topBar: @Composable (() -> Unit)? = null,
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(modifier.fillMaxSize().background(containerColor)) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            BottomSheetScaffoldLayout(
                topBar = topBar,
                body = { content(PaddingValues(bottom = sheetPeekHeight)) },
                snackbarHost = { snackbarHost(scaffoldState.snackbarHostState) },
                sheetOffset = { scaffoldState.bottomSheetState.requireOffset() },
                sheetState = scaffoldState.bottomSheetState,
                bottomSheet = {
                    StandardBottomSheet(
                        state = scaffoldState.bottomSheetState,
                        peekHeight = sheetPeekHeight,
                        sheetMaxWidth = sheetMaxWidth,
                        sheetSwipeEnabled = sheetSwipeEnabled,
                        shape = sheetShape,
                        containerColor = sheetContainerColor,
                        contentColor = sheetContentColor,
                        tonalElevation = sheetTonalElevation,
                        shadowElevation = sheetShadowElevation,
                        dragHandle = sheetDragHandle,
                        content = sheetContent,
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StandardBottomSheet(
    state: SheetState,
    peekHeight: Dp,
    sheetMaxWidth: Dp,
    sheetSwipeEnabled: Boolean,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    dragHandle: @Composable (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val showMotion = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val hideMotion = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val spatialFlingSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()

    SideEffect {
        state.showMotionSpec = showMotion
        state.hideMotionSpec = hideMotion
        state.anchoredDraggableMotionSpec = spatialFlingSpec
    }

    val scope = rememberCoroutineScope()
    val orientation = Orientation.Vertical
    val peekHeightPx = with(LocalDensity.current) { peekHeight.toPx() }
    var isMouseWheelScroll by remember { mutableStateOf(false) }
    val anchoredDraggableFlingBehavior =
        AnchoredDraggableDefaults.flingBehavior(
            state = state.anchoredDraggableState,
            positionalThreshold = { _ -> state.positionalThreshold.invoke() },
            animationSpec = spatialFlingSpec,
        )

    val nestedScroll =
        if (sheetSwipeEnabled) {
            Modifier.nestedScroll(
                remember(state.anchoredDraggableState) {
                    consumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                        sheetState = state,
                        orientation = orientation,
                        flingBehavior = anchoredDraggableFlingBehavior,
                        isMouseWheelScroll = { isMouseWheelScroll },
                    )
                }
            )
        } else {
            Modifier
        }

    Surface(
        modifier =
            Modifier.widthIn(max = sheetMaxWidth)
                .fillMaxWidth()
                .requiredHeightIn(min = peekHeight)
                .then(nestedScroll)
                .draggableAnchors(
                    state.anchoredDraggableState,
                    orientation
                ) { sheetSize, constraints ->
                    val layoutHeight = constraints.maxHeight.toFloat()
                    val sheetHeight = sheetSize.height.toFloat()

                    val newAnchors = DraggableAnchors {
                        val isHiddenAnchorAvailable =
                            sheetHeight == 0f || peekHeightPx == 0f || !state.skipHiddenState

                        val isInitialLayout = state.anchoredDraggableState.anchors.size == 0
                        val isStableAtPartial =
                            state.currentValue == SheetValue.PartiallyExpanded &&
                                    !state.isAnimationRunning

                        val isAmbiguousPartialAllowed =
                            peekHeightPx == 0f && (isInitialLayout || isStableAtPartial)

                        val isPartiallyExpandedAnchorAvailable =
                            !state.skipPartiallyExpanded &&
                                    (peekHeightPx > 0f || isAmbiguousPartialAllowed) &&
                                    peekHeightPx != sheetHeight

                        val isExpandedAnchorAvailable = sheetHeight > 0f

                        require(
                            isHiddenAnchorAvailable ||
                                    isPartiallyExpandedAnchorAvailable ||
                                    isExpandedAnchorAvailable
                        ) {
                            "BottomSheetScaffold: Require at least 1 anchor to be initialized"
                        }

                        if (isPartiallyExpandedAnchorAvailable) {
                            SheetValue.PartiallyExpanded at (layoutHeight - peekHeightPx)
                        }
                        if (isHiddenAnchorAvailable) {
                            SheetValue.Hidden at layoutHeight
                        }
                        if (isExpandedAnchorAvailable) {
                            SheetValue.Expanded at layoutHeight - sheetHeight
                        }
                    }
                    val newTarget =
                        when (val oldTarget = state.targetValue) {
                            SheetValue.Hidden ->
                                if (newAnchors.hasPositionFor(SheetValue.Hidden)) {
                                    SheetValue.Hidden
                                } else {
                                    oldTarget
                                }

                            SheetValue.PartiallyExpanded ->
                                when {
                                    newAnchors.hasPositionFor(SheetValue.PartiallyExpanded) ->
                                        SheetValue.PartiallyExpanded

                                    newAnchors.hasPositionFor(SheetValue.Expanded) -> SheetValue.Expanded
                                    newAnchors.hasPositionFor(SheetValue.Hidden) -> SheetValue.Hidden
                                    else -> oldTarget
                                }

                            SheetValue.Expanded ->
                                if (newAnchors.hasPositionFor(SheetValue.Expanded)) {
                                    SheetValue.Expanded
                                } else {
                                    SheetValue.Hidden
                                }
                        }
                    return@draggableAnchors newAnchors to newTarget
                }
                .onPointerScrollY { isMouseWheelScroll = true }
                .touchAnchoredDraggable(
                    state = state,
                    enabled = sheetSwipeEnabled,
                    flingBehavior = anchoredDraggableFlingBehavior,
                    onTouchPointerInput = { isMouseWheelScroll = false },
                    scope = scope,
                )
                .verticalScaleUp(state),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScaleDown(state)
        ) {
            if (dragHandle != null) {
                val partialExpandActionLabel = BottomSheetPartialExpandDescription
                val dismissActionLabel = BottomSheetDismissDescription
                val expandActionLabel = BottomSheetExpandDescription
                DragHandleWithTooltip(
                    modifier = Modifier.clickable {
                        when (state.currentValue) {
                            SheetValue.Expanded ->
                                scope.launch {
                                    if (!state.skipHiddenState) {
                                        state.hide()
                                    } else {
                                        state.partialExpand()
                                    }
                                }

                            SheetValue.PartiallyExpanded -> scope.launch { state.expand() }

                            else -> scope.launch { state.show() }
                        }
                    }.semantics(mergeDescendants = true) {
                        with(state) {
                            if (anchoredDraggableState.anchors.size > 1 && sheetSwipeEnabled) {
                                if (currentValue == SheetValue.PartiallyExpanded) {
                                    expand(expandActionLabel) {
                                        val canExpand = confirmValueChange(SheetValue.Expanded)
                                        if (canExpand) {
                                            scope.launch { expand() }
                                        }
                                        return@expand canExpand
                                    }
                                } else {
                                    collapse(partialExpandActionLabel) {
                                        val canPartiallyExpand =
                                            confirmValueChange(SheetValue.PartiallyExpanded)
                                        scope.launch { partialExpand() }
                                        return@collapse canPartiallyExpand
                                    }
                                }
                                if (!state.skipHiddenState) {
                                    dismiss(dismissActionLabel) {
                                        val canHide = confirmValueChange(SheetValue.Hidden)
                                        scope.launch { hide() }
                                        return@dismiss canHide
                                    }
                                }
                            }
                        }
                    },
                    content = dragHandle,
                )
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DragHandleWithTooltip(modifier: Modifier, content: @Composable (() -> Unit)) {
    val dragHandleDescription = BottomSheetDragHandleDescription
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TooltipBox(
            modifier = modifier,
            positionProvider =
                TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(dragHandleDescription) } },
            state = rememberTooltipState(),
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetScaffoldLayout(
    topBar: @Composable (() -> Unit)?,
    body: @Composable () -> Unit,
    bottomSheet: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    sheetOffset: () -> Float,
    sheetState: SheetState,
) {
    Layout(
        contents = listOf<@Composable () -> Unit>(topBar ?: {}, body, bottomSheet, snackbarHost)
    ) { (topBarMeasurables, bodyMeasurables, bottomSheetMeasurables, snackbarHostMeasurables),
        constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val sheetPlaceables = bottomSheetMeasurables.fastMap { it.measure(looseConstraints) }

        val topBarPlaceables = topBarMeasurables.fastMap { it.measure(looseConstraints) }
        val topBarHeight = topBarPlaceables.fastMaxOfOrNull { it.height } ?: 0

        val bodyConstraints = looseConstraints.copy(maxHeight = layoutHeight - topBarHeight)
        val bodyPlaceables = bodyMeasurables.fastMap { it.measure(bodyConstraints) }

        val snackbarPlaceables = snackbarHostMeasurables.fastMap { it.measure(looseConstraints) }

        layout(layoutWidth, layoutHeight) {
            val sheetWidth = sheetPlaceables.fastMaxOfOrNull { it.width } ?: 0
            val sheetOffsetX = max(0, (layoutWidth - sheetWidth) / 2)

            val snackbarWidth = snackbarPlaceables.fastMaxOfOrNull { it.width } ?: 0
            val snackbarHeight = snackbarPlaceables.fastMaxOfOrNull { it.height } ?: 0
            val snackbarOffsetX = (layoutWidth - snackbarWidth) / 2
            val snackbarOffsetY =
                when (sheetState.currentValue) {
                    SheetValue.PartiallyExpanded -> sheetOffset().roundToInt() - snackbarHeight
                    SheetValue.Expanded,
                    SheetValue.Hidden -> layoutHeight - snackbarHeight
                }

            bodyPlaceables.fastForEach { it.placeRelative(0, topBarHeight) }
            topBarPlaceables.fastForEach { it.placeRelative(0, 0) }
            sheetPlaceables.fastForEach { it.placeRelative(sheetOffsetX, 0) }
            snackbarPlaceables.fastForEach { it.placeRelative(snackbarOffsetX, snackbarOffsetY) }
        }
    }
}

internal fun <T> Modifier.draggableAnchors(
    state: AnchoredDraggableState<T>,
    orientation: Orientation,
    anchors: (size: IntSize, constraints: Constraints) -> Pair<DraggableAnchors<T>, T>,
) = this then DraggableAnchorsElement(state, anchors, orientation)

private fun Modifier.touchAnchoredDraggable(
    state: SheetState,
    enabled: Boolean,
    flingBehavior: FlingBehavior,
    onTouchPointerInput: () -> Unit,
    scope: CoroutineScope,
): Modifier {
    if (!enabled) return this

    return pointerInput(state, flingBehavior) {
        val velocityTracker = VelocityTracker()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (down.type != PointerType.Touch) {
                return@awaitEachGesture
            }

            onTouchPointerInput()
            velocityTracker.resetTracking()
            velocityTracker.addPointerInputChange(down)

            var overSlop = 0f
            val drag =
                awaitVerticalPointerSlopOrCancellation(down.id, PointerType.Touch) { change, slop ->
                    change.consume()
                    overSlop = slop
                }

            if (drag != null) {
                state.anchoredDraggableState.dispatchRawDelta(overSlop)
                velocityTracker.addPointerInputChange(drag)

                if (
                    verticalDrag(drag.id) { change ->
                        velocityTracker.addPointerInputChange(change)
                        state.anchoredDraggableState.dispatchRawDelta(change.positionChange().y)
                        change.consume()
                    }
                ) {
                    scope.launch {
                        state.anchoredDrag(flingBehavior, velocityTracker.calculateVelocity().y)
                    }
                } else {
                    scope.launch {
                        state.snapTo(state.targetValue)
                    }
                }
            }
        }
    }
}

private class DraggableAnchorsElement<T>(
    private val state: AnchoredDraggableState<T>,
    private val anchors: (size: IntSize, constraints: Constraints) -> Pair<DraggableAnchors<T>, T>,
    private val orientation: Orientation,
) : ModifierNodeElement<DraggableAnchorsNode<T>>() {
    override fun create() = DraggableAnchorsNode(state, anchors, orientation)

    override fun update(node: DraggableAnchorsNode<T>) {
        node.update(state, anchors, orientation)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DraggableAnchorsElement<*>) return false
        if (state != other.state) return false
        if (anchors !== other.anchors) return false
        if (orientation != other.orientation) return false
        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + anchors.hashCode()
        result = 31 * result + orientation.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        debugInspectorInfo {
            properties["state"] = state
            properties["anchors"] = anchors
            properties["orientation"] = orientation
        }
    }
}

private class DraggableAnchorsNode<T>(
    var state: AnchoredDraggableState<T>,
    var anchors: (size: IntSize, constraints: Constraints) -> Pair<DraggableAnchors<T>, T>,
    var orientation: Orientation,
) : Modifier.Node(), LayoutModifierNode {
    private var didInitializeAnchors = false

    override fun onDetach() {
        didInitializeAnchors = false
    }

    private val isReverseDirection: Boolean
        get() = requireLayoutDirection() == LayoutDirection.Rtl && orientation == Orientation.Horizontal

    fun update(
        state: AnchoredDraggableState<T>,
        anchors: (size: IntSize, constraints: Constraints) -> Pair<DraggableAnchors<T>, T>,
        orientation: Orientation,
    ) {
        if (this.state != state) {
            didInitializeAnchors = false
            invalidateMeasurement()
        }
        this.state = state
        this.anchors = anchors
        this.orientation = orientation
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        if (!isLookingAhead || !didInitializeAnchors) {
            val size = IntSize(placeable.width, placeable.height)
            val (newAnchors, suggestedTarget) = anchors(size, constraints)
            state.updateAnchors(newAnchors, suggestedTarget)
            didInitializeAnchors = true
        }

        didInitializeAnchors = isLookingAhead || didInitializeAnchors
        return layout(placeable.width, placeable.height) {
            val offset =
                if (isLookingAhead) {
                    state.anchors.positionOf(state.targetValue)
                } else {
                    state.offset
                }

            checkOffsetIsValid(offset, isLookingAhead)

            val rtlModifier = if (isReverseDirection) -1f else 1f
            val xOffset = if (orientation == Orientation.Horizontal) offset * rtlModifier else 0f
            val yOffset = if (orientation == Orientation.Vertical) offset else 0f
            withMotionFrameOfReferencePlacement {
                placeable.place(xOffset.roundToInt(), yOffset.roundToInt())
            }
        }
    }

    private fun checkOffsetIsValid(offset: Float, isLookingAhead: Boolean) {
        if (offset.isNaN()) {
            throw AnchoredDraggableUninitializedException(
                isLookingAhead = isLookingAhead,
                didLookahead = didInitializeAnchors,
                anchors = state.anchors,
                targetValue = state.targetValue,
            )
        }
    }
}

internal class AnchoredDraggableUninitializedException(
    isLookingAhead: Boolean,
    didLookahead: Boolean,
    anchors: DraggableAnchors<*>,
    targetValue: Any?,
) : Throwable() {
    override val message: String =
        "AnchoredDraggableState was not initialized correctly. " +
                "isLookingAhead=$isLookingAhead,didLookahead=$didLookahead,anchors=$anchors,targetValue=$targetValue"
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun Modifier.verticalScaleUp(state: SheetState) = graphicsLayer {
    val offset = state.anchoredDraggableState.offset
    val anchor = state.anchoredDraggableState.anchors.minPosition()
    val overflow = if (offset < anchor) anchor - offset else 0f
    scaleY = if (overflow > 0f) (size.height + overflow) / size.height else 1f
    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun Modifier.verticalScaleDown(state: SheetState) = graphicsLayer {
    val offset = state.anchoredDraggableState.offset
    val anchor = state.anchoredDraggableState.anchors.minPosition()
    val overflow = if (offset < anchor) anchor - offset else 0f
    scaleY = if (overflow > 0f) 1 / ((size.height + overflow) / size.height) else 1f
    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
}

internal val BottomSheetAnimationSpec: AnimationSpec<Float> =
    tween(durationMillis = 300, easing = FastOutSlowInEasing)

private val DragHandleVerticalPadding = 22.dp

@OptIn(ExperimentalMaterial3Api::class)
internal fun consumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
    sheetState: SheetState,
    orientation: Orientation,
    flingBehavior: FlingBehavior,
    isMouseWheelScroll: () -> Boolean,
): NestedScrollConnection =
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (isMouseWheelScroll()) return Offset.Zero
            val delta = available.toFloat()
            return if (delta < 0 && source == NestedScrollSource.UserInput) {
                sheetState.anchoredDraggableState.dispatchRawDelta(delta).toOffset()
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (isMouseWheelScroll()) return Offset.Zero
            return if (source == NestedScrollSource.UserInput) {
                sheetState.anchoredDraggableState.dispatchRawDelta(available.toFloat()).toOffset()
            } else {
                Offset.Zero
            }
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val toFling = available.toFloat()
            val currentOffset = sheetState.requireOffset()
            val minAnchor = sheetState.anchoredDraggableState.anchors.minPosition()
            return if (toFling < 0 && currentOffset > minAnchor) {
                sheetState.anchoredDrag(flingBehavior, toFling)
                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            val toFling = available.toFloat()
            if (toFling == 0f) {
                sheetState.anchoredDraggableState.snapTo(sheetState.targetValue)
                return Velocity.Zero
            }
            val consumedByAnchoredDraggableFling = sheetState.anchoredDrag(flingBehavior, toFling)
            return Velocity(consumed.x, consumedByAnchoredDraggableFling)
        }

        private fun Float.toOffset(): Offset =
            Offset(
                x = if (orientation == Orientation.Horizontal) this else 0f,
                y = if (orientation == Orientation.Vertical) this else 0f,
            )

        @JvmName("velocityToFloat")
        private fun Velocity.toFloat() = if (orientation == Orientation.Horizontal) x else y

        @JvmName("offsetToFloat")
        private fun Offset.toFloat(): Float = if (orientation == Orientation.Horizontal) x else y
    }

@Composable
@ExperimentalMaterial3Api
internal fun rememberSheetState(
    enabledValues: Set<SheetValue> =
        setOf(SheetValue.Hidden, SheetValue.PartiallyExpanded, SheetValue.Expanded),
    confirmValueChange: (SheetValue) -> Boolean = { true },
    initialValue: SheetValue = SheetValue.Hidden,
    positionalThreshold: Dp = BottomSheetDefaults.PositionalThreshold,
    velocityThreshold: Dp = BottomSheetDefaults.VelocityThreshold,
    isBottomSheetPartiallyExpandedDeterministicEnabled: Boolean = true,
): SheetState {
    val density = LocalDensity.current
    val positionalThresholdToPx = { with(density) { positionalThreshold.toPx() } }
    val velocityThresholdToPx = { with(density) { velocityThreshold.toPx() } }
    return rememberSaveable(
        enabledValues,
        confirmValueChange,
        isBottomSheetPartiallyExpandedDeterministicEnabled,
        saver =
            SheetState.Saver(
                enabledValues = enabledValues,
                positionalThreshold = positionalThresholdToPx,
                velocityThreshold = velocityThresholdToPx,
                confirmValueChange = confirmValueChange,
                isBottomSheetPartiallyExpandedDeterministicEnabled =
                    isBottomSheetPartiallyExpandedDeterministicEnabled,
            ),
    ) {
        SheetState(
            enabledValues = enabledValues,
            positionalThreshold = positionalThresholdToPx,
            velocityThreshold = velocityThresholdToPx,
            initialValue = initialValue,
            confirmValueChange = confirmValueChange,
            isBottomSheetPartiallyExpandedDeterministicEnabled =
                isBottomSheetPartiallyExpandedDeterministicEnabled,
        )
    }
}
