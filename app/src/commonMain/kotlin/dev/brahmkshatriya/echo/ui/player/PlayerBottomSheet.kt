package dev.brahmkshatriya.echo.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.IntState
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.ui.components.BetterSheetScaffold
import dev.brahmkshatriya.echo.ui.components.ProgressState
import dev.brahmkshatriya.echo.ui.components.paddingMask
import dev.brahmkshatriya.echo.ui.components.rememberBetterScaffoldState
import dev.brahmkshatriya.echo.ui.components.rememberProgressState
import dev.brahmkshatriya.echo.ui.components.simpleTween
import kotlinx.coroutines.launch

val LocalPlayerPadding = compositionLocalOf { PaddingValues.Zero }
val LocalPlayerSheet = staticCompositionLocalOf<PlayerSheet?> { null }
val LocalInitialPlayerSheetValue = staticCompositionLocalOf { SheetValue.PartiallyExpanded }

data class PlayerSheet(
    val scaffoldState: BottomSheetScaffoldState,
    val sheetProgressState: ProgressState,
    val sheetOffsetState: ProgressState,
    val peekHeight: State<Dp>,
    val midPoint: IntState,
    val isExpanded: State<Boolean>,
    val sheetState: SheetState = scaffoldState.bottomSheetState,
    val snackBarHostState: SnackbarHostState = scaffoldState.snackbarHostState
)

@Composable
fun Modifier.applyPlayerTranslation() = run {
    val playerSheet = LocalPlayerSheet.current ?: return@run this
    val peekHeight = playerSheet.peekHeight.value
    val midPoint = playerSheet.midPoint.intValue
    val pixelProgress = playerSheet.sheetOffsetState.value
    applyPlayerTranslation(pixelProgress, peekHeight, midPoint)
}

@Composable
fun Modifier.applyPlayerTranslation(
    pixelProgress: Float,
    peekHeight: Dp,
    midPoint: Int
) = run {
    val animated = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    graphicsLayer {
        val threshHold = peekHeight.toPx() / 2
        val y = pixelProgress - midPoint
        scope.launch {
            if (y < threshHold) {
                if (y < 0f) animated.snapTo(y)
                else animated.animateTo(y)
            } else animated.animateTo(0f)
        }
        translationY = animated.value
    }
}

@Composable
fun PlayerBottomSheet(
    startPadding: Dp,
    bottomPadding: Dp,
    content: @Composable () -> Unit,
) {
    val peekHeightState = remember { mutableStateOf(64.dp) }
    val sheetOffsetState = rememberProgressState()
    val sheetProgressState = rememberProgressState()
    val backProgressState = rememberProgressState()
    val midPointState = remember { mutableIntStateOf(0) }
    val isExpandedState = remember { mutableStateOf(false) }
    val scaffoldState = rememberBetterScaffoldState(LocalInitialPlayerSheetValue.current)

    val sheetProgress by sheetProgressState
    val backProgress by backProgressState

    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val actualBottomPadding = safePadding.calculateBottomPadding() + bottomPadding

    val layoutDirection = LocalLayoutDirection.current
    val startPadding = 12.dp + startPadding + safePadding.calculateStartPadding(layoutDirection)
    val endPadding = 12.dp + safePadding.calculateEndPadding(layoutDirection)

    CompositionLocalProvider(
        LocalPlayerSheet provides remember {
            PlayerSheet(
                scaffoldState,
                sheetProgressState,
                sheetOffsetState,
                peekHeightState,
                midPointState,
                isExpandedState
            )
        },
        LocalPlayerPadding provides remember(startPadding, endPadding, bottomPadding) {
            PaddingValues(start = startPadding, end = endPadding, bottom = bottomPadding)
        }
    ) {
        val modifier = Modifier.graphicsLayer {
            val scale = 1 - 0.15f * backProgress
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0.5f, 1f)
        }
        BetterSheetScaffold(
            sheetContent = {
                Box(modifier.fillMaxSize().graphicsLayer {
                    alpha = 1 + sheetProgress.coerceIn(-1f, 0f)
                }) { PlayerItem() }
            },
            bottomPadding = actualBottomPadding,
            sheetProgressState = sheetProgressState,
            sheetOffsetState = sheetOffsetState,
            scaffoldState = scaffoldState,
            backProgressState = backProgressState,
            sheetPeekHeight = peekHeightState.value,
            sheetShape = RectangleShape,
            sheetDragHandle = null,
            sheetShadowElevation = 0.dp,
            sheetContainerColor = Color.Unspecified,
            sheetMaxWidth = Dp.Unspecified,
            containerColor = Color.Unspecified,
            content = { midPoint, isExpanded ->
                midPointState.intValue = midPoint
                isExpandedState.value = isExpanded
                val top = 8.dp
                val peekHeight by peekHeightState
                val positiveProgress = sheetProgress.coerceIn(0f, 1f)

                val animatedStart by animateDpAsState(startPadding, simpleTween())
                val animatedEnd by animateDpAsState(endPadding, simpleTween())
                val shape = remember(
                    peekHeight, backProgress, positiveProgress, animatedStart, animatedEnd
                ) {
                    ClippedShape(
                        peekHeight + top - 8.dp,
                        positiveProgress,
                        backProgress,
                        animatedStart,
                        animatedEnd
                    )
                }
                Box(
                    modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 1 + sheetProgress.coerceIn(-1f, 0f)
                            translationY =
                                sheetOffsetState.value - top.toPx() * (1 - positiveProgress)
                        }
                        .clip(shape)
                        .background(colorScheme.primary)
                )
                Box(Modifier.paddingMask().then(modifier).applyPlayerTranslation()) {
                    content()
                }
            }
        )
    }
}
