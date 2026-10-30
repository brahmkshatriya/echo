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
    backgroundColor: Color = Color.Unspecified,
    progress: () -> Float = { 1f },
): Modifier {
    // An opaque, uniform backdrop can reproduce the fade with SrcOver.
    // Only transparent/unknown backdrops require a full offscreen mask.
    val masked = backgroundColor == Color.Unspecified
    val layer =
        if (masked) graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen } else this
    return layer.drawWithCache {
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
        // DstOut with a fixed gradient and animated opacity is equivalent to
        // DstIn with animated gradient stops. Keep the shader cached while the
        // sheet moves instead of rebuilding it on every frame.
        val fadeStartFraction = 0.25f
        val edgeColor = if (masked) Color.Black else backgroundColor
        val transparentEdge = edgeColor.copy(alpha = 0f)
        val blendMode = if (masked) BlendMode.DstOut else BlendMode.SrcOver
        val topMask =
            if (top > 0f)
                Brush.verticalGradient(
                    0f to edgeColor,
                    fadeStartFraction to edgeColor,
                    1f to transparentEdge,
                    startY = 0f,
                    endY = top,
                )
            else null
        val bottomMask =
            if (bottom > 0f)
                Brush.verticalGradient(
                    0f to transparentEdge,
                    (1f - fadeStartFraction) to edgeColor,
                    1f to edgeColor,
                    startY = size.height - bottom,
                    endY = size.height,
                )
            else null
        val leftMask =
            if (left > 0f)
                Brush.horizontalGradient(
                    0f to edgeColor,
                    fadeStartFraction to edgeColor,
                    1f to transparentEdge,
                    startX = 0f,
                    endX = left,
                )
            else null
        val rightMask =
            if (right > 0f)
                Brush.horizontalGradient(
                    0f to transparentEdge,
                    (1f - fadeStartFraction) to edgeColor,
                    1f to edgeColor,
                    startX = size.width - right,
                    endX = size.width,
                )
            else null

        onDrawWithContent {
            drawContent()

            val maskProgress = progress().coerceIn(0f, 1f)
            if (maskProgress <= 0f) return@onDrawWithContent

            if (topMask != null)
                drawRect(
                    brush = topMask,
                    size = topSize,
                    alpha = maskProgress,
                    blendMode = blendMode,
                )
            if (bottomMask != null)
                drawRect(
                    brush = bottomMask,
                    topLeft = bottomOffset,
                    size = bottomSize,
                    alpha = maskProgress,
                    blendMode = blendMode,
                )
            if (leftMask != null)
                drawRect(
                    brush = leftMask,
                    size = leftSize,
                    alpha = maskProgress,
                    blendMode = blendMode,
                )
            if (rightMask != null)
                drawRect(
                    brush = rightMask,
                    topLeft = rightOffset,
                    size = rightSize,
                    alpha = maskProgress,
                    blendMode = blendMode,
                )
        }
    }
}
