package dev.brahmkshatriya.echo.ui.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

class CircleCutoutShape(
    private val diameter: Dp,
    private val offsetFromEnd: Dp = 0.dp,
    private val offsetFromBottom: Dp = 0.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radiusPx = with(density) { diameter.div(2).toPx() }
        val endOffsetPx = with(density) { offsetFromEnd.toPx() }
        val bottomOffsetPx = with(density) { offsetFromBottom.toPx() }

        val path = Path().apply {
            addRect(Rect(0f, 0f, size.width, size.height))

            val circleCenterX = when (layoutDirection) {
                LayoutDirection.Ltr -> size.width - radiusPx - endOffsetPx
                LayoutDirection.Rtl -> radiusPx + endOffsetPx
            }
            val circleCenterY = size.height - radiusPx - bottomOffsetPx

            addOval(
                Rect(
                    left = circleCenterX - radiusPx,
                    top = circleCenterY - radiusPx,
                    right = circleCenterX + radiusPx,
                    bottom = circleCenterY + radiusPx
                )
            )
        }

        // Use even-odd fill rule to create the cutout
        path.fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd

        return Outline.Generic(path)
    }
}