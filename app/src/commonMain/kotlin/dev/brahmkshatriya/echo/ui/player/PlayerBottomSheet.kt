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
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
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
import dev.brahmkshatriya.echo.ui.components.BetterSheet
import dev.brahmkshatriya.echo.ui.components.BetterSheetScaffold
import dev.brahmkshatriya.echo.ui.components.paddingMask
import kotlinx.coroutines.launch

val LocalPlayerPadding = compositionLocalOf { PaddingValues.Zero }
val LocalPlayerSheet = staticCompositionLocalOf<BetterSheet?> { null }
val LocalInitialPlayerSheetValue = staticCompositionLocalOf { SheetValue.PartiallyExpanded }

@Composable
fun Modifier.applyPlayerTranslation() = run {
    val playerSheet = LocalPlayerSheet.current ?: return@run this
    val animatable = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    graphicsLayer {
        val midPoint = playerSheet.midPointState.intValue
        val pixelProgress = playerSheet.offsetState.floatValue
        val threshHold = playerSheet.peekHeight.toPx() / 3
        scope.launch {
            val y = pixelProgress - midPoint
            if (y < threshHold) {
                if (y < 0f) animatable.snapTo(y)
                else animatable.animateTo(y)
            } else animatable.animateTo(0f)
        }
        translationY = animatable.value
    }
}

@Composable
fun PlayerBottomSheet(
    betterSheet: BetterSheet,
    startPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {

    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val actualBottomPadding = safePadding.calculateBottomPadding() + bottomPadding

    val layoutDirection = LocalLayoutDirection.current
    val startPadding = startPadding + safePadding.calculateStartPadding(layoutDirection)
    val endPadding = safePadding.calculateEndPadding(layoutDirection)

    CompositionLocalProvider(
        LocalPlayerSheet provides betterSheet,
        LocalPlayerPadding provides remember(startPadding, endPadding, bottomPadding) {
            PaddingValues(start = startPadding, end = endPadding, bottom = bottomPadding)
        }
    ) {
        val modifier = Modifier.graphicsLayer {
            val backProgress = betterSheet.backProgressState.floatValue
            val scale = 1 - 0.15f * backProgress
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0.5f, 1f)
        }
        BetterSheetScaffold(
            sheetContent = {
                Box(modifier.fillMaxSize().graphicsLayer {
                    val sheetProgress by betterSheet.progressState
                    alpha = 1 + sheetProgress.coerceIn(-1f, 0f)
                }) {
                    val artWorks = remember {
                        listOf(
                            "https://i1.sndcdn.com/artworks-f5P5EvBt5Qu57jLk-UNArNA-t1080x1080.jpg",
                            "https://i1.sndcdn.com/artworks-mJmURREt59PyaXxx-nhowNw-t1080x1080.png",
                            "https://i1.sndcdn.com/artworks-GzqTFOMbFiXRz5LL-G1R9uA-t1080x1080.jpg",
                            "https://i1.sndcdn.com/artworks-UbVxfud5u7hzFUPc-pxSyCg-t1080x1080.png",
                            "https://i1.sndcdn.com/artworks-7C8GJbswfVyxJ0z6-r5FPkQ-t1080x1080.png"
                        )
                    }
                    val pagerState = rememberPagerState(2, pageCount = { artWorks.size })
                    HorizontalPager(
                        pagerState,
                        Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) {
                        PlayerItem(artWorks[it], it)
                    }
                }
            },
            bottomPadding = actualBottomPadding,
            betterSheet = betterSheet,
            sheetShape = RectangleShape,
            sheetDragHandle = null,
            sheetShadowElevation = 0.dp,
            sheetContainerColor = Color.Unspecified,
            sheetMaxWidth = Dp.Unspecified,
            containerColor = Color.Unspecified,
            content = {
                Box(Modifier.paddingMask().then(modifier).applyPlayerTranslation()) {
                    content()
                }
            }
        )
    }
}
