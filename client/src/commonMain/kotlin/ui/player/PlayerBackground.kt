package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max

class ClippedShape(
    val minHeight: Dp,
    val progress: Float,
    val backProgress: Float,
    val topPadding: Dp,
    val startPadding: Dp,
    val endPadding: Dp,
    val radius: Dp = 16.dp
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val height = density.run {
            val minHeightPx = minHeight.toPx()
            minHeightPx + (size.height - minHeightPx) * progress
        }
        val offset = 1 - progress
        val startPadPx = density.run { startPadding.toPx() * offset }
        val endPadPx = density.run { endPadding.toPx() * offset }
        val topPadPx = density.run { topPadding.toPx() * offset }
        val leftPadding = if (layoutDirection == LayoutDirection.Ltr) startPadPx else endPadPx
        val rightPadding = if (layoutDirection == LayoutDirection.Ltr) endPadPx else startPadPx

        val rectWidth = max(0f, size.width - leftPadding - rightPadding)

        val cornerRadius = density.run {
            val radiusPx = radius.toPx()
            max(radiusPx * offset, radiusPx * 2 * backProgress)
        }

        return Outline.Rounded(
            RoundRect(
                Rect(Offset(leftPadding, topPadPx), Size(rectWidth, height - topPadPx)),
                CornerRadius(cornerRadius)
            )
        )
    }
}

@Composable
fun Modifier.playerBackground(
    backgroundColor: Color? = null,
    outlineColor: Color? = null,
): Modifier {
    val playerSheet = LocalPlayerSheet.current
    val playerPadding = LocalPlayerPadding.current
    val peekHeight = playerSheet?.peekHeight ?: 80.dp
    val layoutDirection = LocalLayoutDirection.current
    val startPadding = playerPadding.calculateStartPadding(layoutDirection)
    val endPadding = playerPadding.calculateEndPadding(layoutDirection)
    val animatedStart = animateDpAsState(
        startPadding + collapsedHorizontalPadding.dp, tween()
    )
    val animatedEnd = animateDpAsState(
        endPadding + collapsedHorizontalPadding.dp, tween()
    )

    return fillMaxSize().graphicsLayer {
        val sheetProgress = playerSheet?.progressState?.floatValue ?: 0f
        val positiveProgress = sheetProgress.coerceIn(0f, 1f)
        val backProgress = playerSheet?.backProgressState?.floatValue ?: 0f
        clip = true
        shape = ClippedShape(
            peekHeight - 8.dp,
            positiveProgress,
            backProgress,
            8.dp,
            animatedStart.value,
            animatedEnd.value
        )
    }.run {
        val color = backgroundColor ?: return@run this
        val withBackground = background(color)
        if (outlineColor != null) withBackground.drawWithCache {
            val minHeightPx = (peekHeight - 8.dp).toPx()
            val topPaddingPx = 8.dp.toPx()
            val cornerRadiusPx = 16.dp.toPx()
            val maxStrokePx = 0.5.dp.toPx()
            onDrawWithContent {
                drawContent()
                val progress = (playerSheet?.progressState?.floatValue ?: 0f).coerceIn(0f, 1f)
                val collapsed = 1f - progress
                val strokeWidth = maxStrokePx * collapsed
                if (strokeWidth <= 0f) return@onDrawWithContent

                // Same bounds/radius as ClippedShape, without allocating a
                // second Shape and Outline on every drag frame.
                val startInset = animatedStart.value.toPx() * collapsed
                val endInset = animatedEnd.value.toPx() * collapsed
                val leftInset = if (layoutDirection == LayoutDirection.Ltr) startInset else endInset
                val rightInset = if (layoutDirection == LayoutDirection.Ltr) endInset else startInset
                val topInset = topPaddingPx * collapsed
                val height = minHeightPx + (size.height - minHeightPx) * progress
                val width = size.width - leftInset - rightInset - strokeWidth
                val outlineHeight = height - topInset - strokeWidth
                if (width <= 0f || outlineHeight <= 0f) return@onDrawWithContent

                val backProgress = playerSheet?.backProgressState?.floatValue ?: 0f
                val radius = max(cornerRadiusPx * collapsed, cornerRadiusPx * 2f * backProgress)
                val halfStroke = strokeWidth / 2f
                drawRoundRect(
                    color = outlineColor,
                    topLeft = Offset(leftInset + halfStroke, topInset + halfStroke),
                    size = Size(width, outlineHeight),
                    cornerRadius = CornerRadius((radius - halfStroke).coerceAtLeast(0f)),
                    style = Stroke(width = strokeWidth),
                )
            }
        } else withBackground
    }
}
