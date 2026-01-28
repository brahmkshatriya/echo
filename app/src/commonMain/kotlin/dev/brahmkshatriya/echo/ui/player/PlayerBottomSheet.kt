package dev.brahmkshatriya.echo.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.brahmkshatriya.echo.ui.components.BetterSheetScaffold
import dev.brahmkshatriya.echo.ui.components.ProgressState
import dev.brahmkshatriya.echo.ui.components.rememberProgressState
import kotlinx.coroutines.launch

val LocalSnackBarHostState = staticCompositionLocalOf<SnackbarHostState?> { null }

val LocalPlayerUi = staticCompositionLocalOf<PlayerUi?> { null }
val LocalInitialPlayerSheetValue = staticCompositionLocalOf { SheetValue.PartiallyExpanded }

class PlayerUi(
    val sheetState: SheetState,
    val sheetProgressState: ProgressState,
    val sheetPixelsProgressState: ProgressState,
    val peekHeight: Dp,
    val midPoint: Int,
    val isExpanded: Boolean,
)

@Composable
fun Modifier.applyPlayerTranslation() = run {
    val playerUi = LocalPlayerUi.current ?: return@run this
    val peekHeight = playerUi.peekHeight
    val bottomPadding = playerUi.midPoint
    val pixelProgress = playerUi.sheetPixelsProgressState.value
    applyPlayerTranslation(pixelProgress, peekHeight, bottomPadding)
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
        val y = -midPoint + pixelProgress
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
    extraPadding: Dp,
    content: @Composable () -> Unit,
) {
    val bottomSheetState = rememberStandardBottomSheetState(
        LocalInitialPlayerSheetValue.current,
        skipHiddenState = false
    )
    val sheetProgressState = rememberProgressState()
    val sheetPixelsProgressState = rememberProgressState()

    val padding = WindowInsets.safeContent.asPaddingValues()
    val peekHeight = 64.dp
    val backProgressState = rememberProgressState()

    val sheetProgress by sheetProgressState
    val backProgress by backProgressState

    val systemBottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    val bottomPadding = systemBottomPadding + extraPadding

    val snackBarHostState = remember { SnackbarHostState() }
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState, snackBarHostState)
    BetterSheetScaffold(
        sheetContent = {
            val density = LocalDensity.current
            val paddingTop = density.run { padding.calculateTopPadding().toPx() }
            val negativeProgress = sheetProgress.coerceAtMost(0f)
            val positiveProgress = sheetProgress.coerceAtLeast(0f)
            Column(
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = 1 + negativeProgress
                    translationY = positiveProgress * paddingTop
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Bottom sheet", fontSize = (16 + 32 * positiveProgress).sp)
            }
        },
        modifier = Modifier.graphicsLayer {
            val scale = 1 - 0.15f * backProgress
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0.5f, 1f)
        },
        bottomPadding = bottomPadding,
        sheetProgressState = sheetProgressState,
        sheetPixelsProgressState = sheetPixelsProgressState,
        scaffoldState = scaffoldState,
        backProgressState = backProgressState,
        sheetState = bottomSheetState,
        sheetPeekHeight = peekHeight,
        sheetShape = RectangleShape,
        sheetDragHandle = null,
        sheetShadowElevation = 0.dp,
        sheetContainerColor = Color.Unspecified,
        sheetMaxWidth = Dp.Unspecified,
        containerColor = Color.Unspecified,
        content = { midPoint, isExpanded ->
            CompositionLocalProvider(
                LocalPlayerUi provides PlayerUi(
                    bottomSheetState,
                    sheetProgressState,
                    sheetPixelsProgressState,
                    peekHeight,
                    midPoint,
                    isExpanded
                ),
                LocalSnackBarHostState provides snackBarHostState
            ) {
                Box(Modifier.applyPlayerTranslation()){ content() }
            }
        }
    )
}