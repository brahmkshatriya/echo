package dev.brahmkshatriya.echo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch

private object Content : NavigationEventInfo()
private object BottomSheet : NavigationEventInfo()

class ProgressState : State<Float> {
    internal val mutableState = mutableFloatStateOf(0f)
    override val value: Float
        get() = mutableState.floatValue
}

@Composable
fun rememberProgressState() = remember { ProgressState() }

fun sheetProgress(
    sheetProgressState: ProgressState,
    pixels: Float,
    height: Int,
    sheetPeekHeight: Int,
) {
    val maxOffset = height - sheetPeekHeight
    val progress = if (pixels < maxOffset) 1 - pixels / maxOffset
    else (maxOffset - pixels) / (height - maxOffset)
    sheetProgressState.mutableState.floatValue = progress
}

@Composable
fun BetterSheetScaffold(
    sheetContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding(),
    sheetProgressState: ProgressState = rememberProgressState(),
    sheetPixelsProgressState: ProgressState = rememberProgressState(),
    backProgressState: ProgressState = rememberProgressState(),
    sheetState: SheetState = rememberStandardBottomSheetState(skipHiddenState = false),
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(sheetState),
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
    snackBarHost: @Composable ((SnackbarHostState) -> Unit) = { SnackbarHost(it) },
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable ((containerHeight:Int, isExpanded: Boolean) -> Unit),
) {
    val animatedBottomPadding by animateDpAsState(bottomPadding, simpleTween())
    val newPeekHeight = sheetPeekHeight + animatedBottomPadding
    val heightState = remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val progress =
        runCatching { sheetState.requireOffset() }.getOrNull() ?: 0f
    sheetPixelsProgressState.mutableState.floatValue = progress
    sheetProgress(
        sheetProgressState,
        progress,
        heightState.intValue,
        with(density) { newPeekHeight.roundToPx() }
    )

    val isExpanded by remember(sheetProgressState) {
        derivedStateOf {
            sheetProgressState.value > 0.6f
        }
    }
    val completeAnimatable = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val navigationEventState = rememberNavigationEventState(
        currentInfo = if (isExpanded) BottomSheet else Content,
        backInfo = if (isExpanded) listOf(Content) else emptyList()
    )

    if (LocalNavigationEventDispatcherOwner.current != null) NavigationBackHandler(
        navigationEventState,
        isBackEnabled = isExpanded,
        onBackCompleted = {
            scope.launch { sheetState.show() }
            scope.launch {
                completeAnimatable.snapTo(1f)
                completeAnimatable.animateTo(0f)
            }
        },
        onBackCancelled = {
            scope.launch { sheetState.expand() }
            scope.launch { completeAnimatable.snapTo(0f) }
        })

    val backProgress = when (val state = navigationEventState.transitionState) {
        NavigationEventTransitionState.Idle -> 0f
        is NavigationEventTransitionState.InProgress -> state.latestEvent.progress
    }
    backProgressState.mutableState.floatValue = maxOf(backProgress, completeAnimatable.value)

    BottomSheetScaffold(
        sheetContent = {
            Box(Modifier.fillMaxSize()) { sheetContent() }
        },
        modifier = modifier.onSizeChanged {
            heightState.intValue = it.height
        },
        scaffoldState = scaffoldState,
        sheetPeekHeight = newPeekHeight,
        sheetShape = sheetShape,
        sheetContainerColor = sheetContainerColor,
        sheetContentColor = sheetContentColor,
        sheetDragHandle = sheetDragHandle,
        sheetMaxWidth = sheetMaxWidth,
        sheetShadowElevation = sheetShadowElevation,
        sheetTonalElevation = sheetTonalElevation,
        sheetSwipeEnabled = sheetSwipeEnabled,
        topBar = topBar,
        snackbarHost = snackBarHost,
        containerColor = containerColor,
        contentColor = contentColor,
        content = {
            val midPoint = density.run {
                heightState.intValue - newPeekHeight.roundToPx()
            }
            content(midPoint,isExpanded)
        })
}