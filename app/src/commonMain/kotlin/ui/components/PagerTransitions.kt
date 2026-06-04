package dev.brahmkshatriya.echo.app.ui.components

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.absoluteValue

fun Modifier.blurFadePagerTransition(
    pagerState: PagerState,
    page: Int,
    blurProgress: () -> Float = { 1f },
) = run {
    val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
    val progress = offset.absoluteValue.coerceIn(0f, 1f)
    zIndex(1f - progress).graphicsLayer {
        translationX = offset * size.width
        alpha = 1f - progress
        val transformedBlurProgress = progress * blurProgress().coerceIn(0f, 1f)
        val radius = 24.dp * transformedBlurProgress
        val horizontalBlurPixels = radius.toPx()
        val verticalBlurPixels = radius.toPx()
        val shouldBlur = horizontalBlurPixels > 0f && verticalBlurPixels > 0f
        this.renderEffect = if (!shouldBlur) null else
            BlurEffect(horizontalBlurPixels, verticalBlurPixels, TileMode.Decal)
    }
}
