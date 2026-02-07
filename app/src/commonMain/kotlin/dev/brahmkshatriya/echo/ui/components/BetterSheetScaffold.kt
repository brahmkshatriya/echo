@file:OptIn(ExperimentalMaterial3Api::class)

package dev.brahmkshatriya.echo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch

object Content : NavigationEventInfo()
object BottomSheet : NavigationEventInfo()

class ProgressState : State<Float> {
    internal val mutableState = mutableFloatStateOf(0f)
    override val value: Float
        get() = mutableState.floatValue
}

@Composable
fun rememberProgressState() = remember { ProgressState() }

@Composable
fun rememberBetterScaffoldState(
    initialValue: SheetValue = SheetValue.PartiallyExpanded
) = rememberBottomSheetScaffoldState(
    rememberStandardBottomSheetState(
        initialValue = initialValue,
        skipHiddenState = false
    )
)

@Suppress("AssignedValueIsNeverRead")
@Composable
fun BetterSheetScaffold(
    sheetContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding(),
    sheetProgressState: ProgressState = rememberProgressState(),
    sheetOffsetState: ProgressState = rememberProgressState(),
    backProgressState: ProgressState = rememberProgressState(),
    scaffoldState: BottomSheetScaffoldState = rememberBetterScaffoldState(),
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
    content: @Composable ((midPoint: Int, isExpanded: Boolean) -> Unit),
) {
    val density = LocalDensity.current
    val sheetState = scaffoldState.bottomSheetState
    var layoutHeight by remember { mutableIntStateOf(0) }
    val animatedBottomPadding by animateDpAsState(bottomPadding, simpleTween())
    val newPeekHeight = sheetPeekHeight + animatedBottomPadding
    val sheetPeekHeight = density.run { newPeekHeight.roundToPx() }
    val offset = runCatching { sheetState.requireOffset() }.getOrNull() ?: 0f

    val maxOffset = layoutHeight - sheetPeekHeight
    val progress = if (offset < maxOffset) 1 - offset / maxOffset
    else (maxOffset - offset) / (layoutHeight - maxOffset)

    sheetOffsetState.mutableState.floatValue = offset
    sheetProgressState.mutableState.floatValue = progress
    val isExpanded = progress > 0.1f

    val completeAnimatable = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val navigationEventState = rememberNavigationEventState(
        currentInfo = if (isExpanded) BottomSheet else Content,
        backInfo = if (isExpanded) listOf(Content) else emptyList()
    )
    NavigationBackHandler(
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
        }
    )

    val backProgress = when (val state = navigationEventState.transitionState) {
        NavigationEventTransitionState.Idle -> 0f
        is NavigationEventTransitionState.InProgress -> state.latestEvent.progress
    }
    backProgressState.mutableState.floatValue = maxOf(backProgress, completeAnimatable.value)

    BottomSheetScaffold(
        sheetContent = { Box(Modifier.fillMaxSize()){ sheetContent() } },
        modifier = modifier.onSizeChanged {
            layoutHeight = it.height
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
            content(maxOffset, isExpanded)
        }
    )
}
