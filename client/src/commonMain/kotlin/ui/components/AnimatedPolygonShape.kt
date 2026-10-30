package dev.brahmkshatriya.echo.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.ripple
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

/**
 * Keep the press ripple's indication independent of the changing polygon Shape. The parent icon
 * button clips it to the currently animated polygon outline.
 */
@Composable
fun PolygonIconRipple(
    interactionSource: MutableInteractionSource,
    content: @Composable () -> Unit,
) {
    val rippleColor = LocalContentColor.current
    Box(
        modifier =
            Modifier.fillMaxSize()
                .indication(
                    interactionSource = interactionSource,
                    indication = ripple(color = rippleColor),
                ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** A polygon morph that can be used as the clickable surface of an icon button. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class MorphingPolygonShape(morph: Morph, progress: Float) : Shape {
    private val normalizedPath = morph.toPath(progress)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path().apply { addPath(normalizedPath) }
        path.transform(Matrix().apply { scale(size.width, size.height) })
        path.translate(size.center - path.getBounds().center)
        return Outline.Generic(path)
    }
}

/**
 * Material 3's built-in icon button shapes only interpolate CornerBasedShapes. MaterialShapes
 * polygons need a Morph to animate between their actual outlines.
 *
 * Progress 0 = rest, 1 = pressed, 2 = checked. Keeping pressed in the middle makes press/release
 * smooth whether the toggle is checked or unchecked.
 */
@Composable
fun animatedPolygonIconShape(
    shape: RoundedPolygon,
    pressedShape: RoundedPolygon,
    interactionSource: MutableInteractionSource,
    checked: Boolean = false,
    checkedShape: RoundedPolygon? = null,
): Shape {
    val pressed by interactionSource.collectIsPressedAsState()
    val target =
        when {
            pressed -> 1f
            checked && checkedShape != null -> 2f
            else -> 0f
        }
    val progress by
        animateFloatAsState(
            targetValue = target,
            animationSpec = motionScheme.fastEffectsSpec(),
            label = "Material polygon icon button shape",
        )
    val morphToPressed = remember(shape, pressedShape) { Morph(shape, pressedShape) }
    val morphToChecked =
        remember(pressedShape, checkedShape) { checkedShape?.let { Morph(pressedShape, it) } }
    return remember(morphToPressed, morphToChecked, progress) {
        if (progress > 1f && morphToChecked != null)
            MorphingPolygonShape(morphToChecked, (progress - 1f).coerceIn(0f, 1f))
        else MorphingPolygonShape(morphToPressed, progress.coerceIn(0f, 1f))
    }
}
