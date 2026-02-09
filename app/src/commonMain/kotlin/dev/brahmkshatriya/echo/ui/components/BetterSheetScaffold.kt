@file:OptIn(ExperimentalMaterial3Api::class)

package dev.brahmkshatriya.echo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object Content : NavigationEventInfo()
object BottomSheet : NavigationEventInfo()


@Composable
fun rememberBetterSheet(
    peekHeight: Dp = BottomSheetDefaults.SheetPeekHeight,
    initialSheetValue: SheetValue = SheetValue.PartiallyExpanded
): BetterSheet {
    val scaffoldState = rememberBottomSheetScaffoldState(
        rememberStandardBottomSheetState(
            initialValue = initialSheetValue,
            skipHiddenState = false
        )
    )
    return remember { BetterSheet(scaffoldState, peekHeight) }
}

class BetterSheet(
    val scaffoldState: BottomSheetScaffoldState,
    val peekHeight: Dp,
    val progressState: MutableFloatState = mutableFloatStateOf(0f),
    val offsetState: MutableFloatState = mutableFloatStateOf(0f),
    val backProgressState: MutableFloatState = mutableFloatStateOf(0f),
    val midPointState: MutableIntState = mutableIntStateOf(0),
    val isExpandedState: MutableState<Boolean> = mutableStateOf(false),
    val sheetState: SheetState = scaffoldState.bottomSheetState,
    val snackBarHostState: SnackbarHostState = scaffoldState.snackbarHostState
)

@Composable
fun BetterSheetScaffold(
    sheetContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    betterSheet: BetterSheet,
    bottomPadding: Dp = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding(),
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
    content: @Composable (PaddingValues) -> Unit,
) {
    val sheetState = betterSheet.sheetState
    val animatedBottomPadding by animateDpAsState(bottomPadding, simpleTween())
    val newPeekHeight = betterSheet.peekHeight + animatedBottomPadding

    val isExpanded by betterSheet.isExpandedState
    val navigationEventState = rememberNavigationEventState(
        currentInfo = if (isExpanded) BottomSheet else Content,
        backInfo = if (isExpanded) listOf(Content) else emptyList()
    )
    val completeAnimatable = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
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

    LaunchedEffect(navigationEventState, completeAnimatable) {
        snapshotFlow {
            val progress = when (val state = navigationEventState.transitionState) {
                NavigationEventTransitionState.Idle -> 0f
                is NavigationEventTransitionState.InProgress -> state.latestEvent.progress
            }
            maxOf(progress, completeAnimatable.value)
        }.collectLatest {
            betterSheet.backProgressState.floatValue = it
        }
    }

    Layout(contents = listOf({
        BottomSheetScaffold(
            sheetContent = { Box(Modifier.fillMaxSize()) { sheetContent() } },
            modifier = modifier,
            scaffoldState = betterSheet.scaffoldState,
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
            content = content
        )
    })) { (measurables), constraints ->
        val layoutHeight = constraints.maxHeight

        val midPoint = layoutHeight - newPeekHeight.roundToPx()
        betterSheet.midPointState.intValue = midPoint

        val offset = runCatching { sheetState.requireOffset() }.getOrNull() ?: 0f
        betterSheet.offsetState.floatValue = offset

        val progress = if (offset < midPoint) 1f - offset / midPoint
        else (midPoint - offset) / (layoutHeight - midPoint)

        betterSheet.progressState.floatValue = progress
        betterSheet.isExpandedState.value = progress > 0.1f

        val placeables = measurables.fastMap { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.fastMap { it.placeRelative(0, 0) }
        }
    }
}