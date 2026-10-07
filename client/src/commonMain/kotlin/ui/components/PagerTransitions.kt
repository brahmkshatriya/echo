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
    val pageZIndex = if (pagerState.currentPage == page) 1f else 0f
    zIndex(pageZIndex).graphicsLayer {
        val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        val progress = offset.absoluteValue.coerceIn(0f, 1f)
        translationX = offset * size.width
        val pageAlpha = 1f - progress
        alpha = pageAlpha

        if (progress <= 0f || pageAlpha <= 0f) {
            renderEffect = null
        } else {
            val transformedBlurProgress = progress * blurProgress().coerceIn(0f, 1f)
            val radiusPixels = 24.dp.toPx() * transformedBlurProgress
            renderEffect = if (radiusPixels <= 0f) null else
                BlurEffect(radiusPixels, radiusPixels, TileMode.Decal)
        }
    }
}
