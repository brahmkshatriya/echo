package dev.brahmkshatriya.echo.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

val WindowInsets.Companion.forDisplayPadding: PaddingValues
    @Composable get() = systemBars.union(displayCutout).asPaddingValues()

@Composable
fun Modifier.paddingMask(
    padding: PaddingValues = WindowInsets.forDisplayPadding,
    progress: () -> Float = { 1f }
): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithCache {
    val top = padding.calculateTopPadding().toPx().coerceIn(0f, size.height)
    val bottom = padding.calculateBottomPadding().toPx().coerceIn(0f, size.height)
    val left = padding.calculateLeftPadding(layoutDirection).toPx().coerceIn(0f, size.width)
    val right = padding.calculateRightPadding(layoutDirection).toPx().coerceIn(0f, size.width)

    val topSize = Size(size.width, top)
    val bottomSize = Size(size.width, bottom)
    val leftSize = Size(left, size.height)
    val rightSize = Size(right, size.height)
    val bottomOffset = Offset(0f, size.height - bottom)
    val rightOffset = Offset(size.width - right, 0f)

    onDrawWithContent {
        drawContent()

        val maskProgress = progress().coerceIn(0f, 1f)
        if (maskProgress <= 0f) return@onDrawWithContent

        val edgeAlpha = 1f - maskProgress
        val edgeColor = Color.Black.copy(alpha = edgeAlpha)
        val opaqueColor = Color.Black
        val fadeStartFraction = 0.25f

        if (top > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to edgeColor,
                        fadeStartFraction to edgeColor,
                        1f to opaqueColor,
                    ),
                    startY = 0f,
                    endY = top,
                ),
                size = topSize,
                blendMode = BlendMode.DstIn,
            )
        }
        if (bottom > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to opaqueColor,
                        (1f - fadeStartFraction) to edgeColor,
                        1f to edgeColor,
                    ),
                    startY = size.height - bottom,
                    endY = size.height,
                ),
                topLeft = bottomOffset,
                size = bottomSize,
                blendMode = BlendMode.DstIn,
            )
        }
        if (left > 0f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to edgeColor,
                        fadeStartFraction to edgeColor,
                        1f to opaqueColor,
                    ),
                    startX = 0f,
                    endX = left,
                ),
                size = leftSize,
                blendMode = BlendMode.DstIn,
            )
        }
        if (right > 0f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to opaqueColor,
                        (1f - fadeStartFraction) to edgeColor,
                        1f to edgeColor,
                    ),
                    startX = size.width - right,
                    endX = size.width,
                ),
                topLeft = rightOffset,
                size = rightSize,
                blendMode = BlendMode.DstIn,
            )
        }
    }
}
