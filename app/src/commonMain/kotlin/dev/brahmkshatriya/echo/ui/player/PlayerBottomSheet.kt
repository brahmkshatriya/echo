package dev.brahmkshatriya.echo.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomSheetScaffoldState
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
import kotlinx.coroutines.launch

val LocalPlayerPadding = compositionLocalOf { PaddingValues.Zero }
val LocalPlayerSheet = staticCompositionLocalOf<PlayerSheet?> { null }
val LocalInitialPlayerSheetValue = staticCompositionLocalOf { SheetValue.PartiallyExpanded }

data class PlayerSheet(
    val scaffoldState: BottomSheetScaffoldState,
    val sheetProgressState: ProgressState,
    val sheetOffsetState: ProgressState,
    val backProgressState: ProgressState,
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
    val peekHeightState = remember { mutableStateOf(72.dp) }
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
    val startPadding = startPadding + safePadding.calculateStartPadding(layoutDirection)
    val endPadding = safePadding.calculateEndPadding(layoutDirection)

    CompositionLocalProvider(
        LocalPlayerSheet provides remember {
            PlayerSheet(
                scaffoldState,
                sheetProgressState,
                sheetOffsetState,
                backProgressState,
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
                }) {
                    val artWorks = listOf(
                        "https://i1.sndcdn.com/artworks-f5P5EvBt5Qu57jLk-UNArNA-t1080x1080.jpg",
                        "https://i1.sndcdn.com/artworks-mJmURREt59PyaXxx-nhowNw-t1080x1080.png",
                        "https://i1.sndcdn.com/artworks-GzqTFOMbFiXRz5LL-G1R9uA-t1080x1080.jpg",
                        "https://i1.sndcdn.com/artworks-UbVxfud5u7hzFUPc-pxSyCg-t1080x1080.png",
                        "https://i1.sndcdn.com/artworks-7C8GJbswfVyxJ0z6-r5FPkQ-t1080x1080.png"
                    )
                    val pagerState = rememberPagerState(2, pageCount = { artWorks.size })
                    HorizontalPager(pagerState) {
                        Box { PlayerItem(artWorks[it], it) }
                    }
                }
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
                Box(Modifier.paddingMask().then(modifier).applyPlayerTranslation()) {
                    content()
                }
            }
        )
    }
}
