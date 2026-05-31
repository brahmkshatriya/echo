package dev.brahmkshatriya.echo.app.ui.components

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

fun Modifier.depthPagerTransition(
    pagerState: PagerState,
    page: Int,
    transitionProgress: Float = 1f,
) = graphicsLayer {
    val effectProgress = transitionProgress.coerceIn(0f, 1f)
    val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
    val progress = offset.absoluteValue.coerceIn(0f, 1f)
    val transformedProgress = progress * effectProgress
    val signedProgress = offset.coerceIn(-1f, 1f)

    cameraDistance = 16f * density
    rotationY = signedProgress * 22f * effectProgress
    scaleX = 1f - (0.08f * transformedProgress)
    scaleY = 1f - (0.08f * transformedProgress)
    alpha = 1f - (0.08f * transformedProgress)
    transformOrigin = TransformOrigin(
        pivotFractionX = if (offset > 0f) 0f else 1f,
        pivotFractionY = 0.5f
    )

    clip = transformedProgress > 0.01f
    shape = RoundedCornerShape(28.dp * transformedProgress)
}
