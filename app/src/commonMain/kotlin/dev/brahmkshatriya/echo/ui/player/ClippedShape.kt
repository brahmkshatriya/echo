package dev.brahmkshatriya.echo.ui.player

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max

class ClippedShape(
    val minHeight: Dp,
    val progress: Float,
    val backProgress: Float,
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

        val leftPadding = if (layoutDirection == LayoutDirection.Ltr) startPadPx else endPadPx
        val rightPadding = if (layoutDirection == LayoutDirection.Ltr) endPadPx else startPadPx

        val rectWidth = max(0f, size.width - leftPadding - rightPadding)

        val cornerRadius = density.run { radius.toPx() * max(offset, backProgress) }

        return Outline.Rounded(
            RoundRect(
                Rect(Offset(leftPadding, 0f), Size(rectWidth, height)),
                CornerRadius(cornerRadius)
            )
        )
    }
}