package dev.brahmkshatriya.echo.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** A progress-ring icon button whose outline morphs only while pressed. */
@Composable
internal fun MorphingProgressIconButton(
    progress: Float,
    onClick: () -> Unit,
    enabled: Boolean,
    backgroundColor: Color,
    progressColor: Color,
    buttonColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shapeProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "Progress button shape",
    )
    val morph = shapeProgress.coerceIn(0f, 1f)
    val outerCorner = 23.dp - 12.dp * morph
    val innerCorner = 19.dp - 12.dp * morph

    Box(
        modifier = modifier
            .size(46.dp)
            .clip(RoundedCornerShape(outerCorner))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val fraction = progress.coerceIn(0f, 1f)
            val stroke = 1.5.dp.toPx()
            val centerX = size.width / 2f
            val centerY = size.height / 2f

            drawRoundRect(
                color = backgroundColor,
                cornerRadius = CornerRadius(outerCorner.toPx()),
            )

            val innerDiameter = 38.dp.toPx()
            drawRoundRect(
                color = buttonColor,
                topLeft = Offset(centerX - innerDiameter / 2f, centerY - innerDiameter / 2f),
                size = Size(innerDiameter, innerDiameter),
                cornerRadius = CornerRadius(innerCorner.toPx()),
            )

            if (fraction > 0f) {
                val pathHalfSize = (44.dp.toPx() - stroke) / 2f
                val left = centerX - pathHalfSize
                val top = centerY - pathHalfSize
                val right = centerX + pathHalfSize
                val bottom = centerY + pathHalfSize
                val radius = ((right - left) / 2f - 12.dp.toPx() * morph)
                    .coerceIn(0f, (right - left) / 2f)

                val outline = Path().apply {
                    val diameter = radius * 2f
                    moveTo(centerX, top)
                    lineTo(right - radius, top)
                    arcTo(Rect(right - diameter, top, right, top + diameter), 270f, 90f, false)
                    lineTo(right, bottom - radius)
                    arcTo(Rect(right - diameter, bottom - diameter, right, bottom), 0f, 90f, false)
                    lineTo(left + radius, bottom)
                    arcTo(Rect(left, bottom - diameter, left + diameter, bottom), 90f, 90f, false)
                    lineTo(left, top + radius)
                    arcTo(Rect(left, top, left + diameter, top + diameter), 180f, 90f, false)
                    close()
                }
                val measure = PathMeasure().apply { setPath(outline, forceClosed = false) }
                val segment = Path()
                measure.getSegment(0f, measure.length * fraction, segment, startWithMoveTo = true)
                drawPath(segment, progressColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
        }
        content()
    }
}
