package dev.brahmkshatriya.echo.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

class PaddingRoundedCornerShape(
    private val topPadding: Dp = 0.dp,
    private val horizontalPadding: Dp = 0.dp,
    private val cornerRadius: Dp = 16.dp
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val topPaddingPx = with(density) { topPadding.toPx() }
        val horizontalPaddingPx = with(density) { horizontalPadding.toPx() }
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }

        return Outline.Rounded(
            RoundRect(
                left = horizontalPaddingPx,
                top = topPaddingPx,
                right = size.width - horizontalPaddingPx,
                bottom = size.height,
                cornerRadius = CornerRadius(cornerRadiusPx)
            )
        )
    }
}