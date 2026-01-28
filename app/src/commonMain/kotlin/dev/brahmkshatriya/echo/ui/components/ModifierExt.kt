package dev.brahmkshatriya.echo.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

val WindowInsets.Companion.forDisplayPadding: PaddingValues
    @Composable get() = systemBars.union(displayCutout).asPaddingValues()

private fun PaddingValues.inPx(density: Density, size: PaddingValues.() -> Dp) =
    density.run { size().toPx() }

@Composable
fun Modifier.paddingMask(
    padding: PaddingValues = WindowInsets.forDisplayPadding,
): Modifier = run {
    val density = LocalDensity.current
    graphicsLayer(alpha = 0.99f).drawWithContent {
        drawContent()

        val top = padding.inPx(density) { calculateTopPadding() }
        val bottom = padding.inPx(density) { calculateBottomPadding() }
        val left = padding.inPx(density) { calculateLeftPadding(LayoutDirection.Ltr) }
        val right = padding.inPx(density) { calculateRightPadding(LayoutDirection.Ltr) }

        val maskColor = Color.Black.copy(alpha = 0.5f)

        fun mask(size: Size, offset: Offset) {
            if (size.width > 0f && size.height > 0f) {
                drawRect(
                    color = maskColor,
                    size = size,
                    topLeft = offset,
                    blendMode = BlendMode.DstIn
                )
            }
        }

        mask(Size(size.width, top), Offset.Zero)
        mask(Size(size.width, bottom), Offset(0f, size.height - bottom))
        mask(Size(left, size.height), Offset.Zero)
        mask(Size(right, size.height), Offset(size.width - right, 0f))
    }
}
