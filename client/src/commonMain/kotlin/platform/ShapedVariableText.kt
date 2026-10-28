package dev.brahmkshatriya.echo.app.platform

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import echo.client.generated.resources.GoogleSansFlex
import echo.client.generated.resources.Res
import org.jetbrains.compose.resources.Font

/** A line is shaped as a complete run: UTF-16 units are never drawn as individual glyphs. */
private data class ShapedLine(val start: Int, val end: Int, val baseline: Float, val x: Float)

/** Complex scripts retain native shaping, fallback fonts, bidi order, and complete emoji clusters. */
@Composable
internal fun ShapedVariableText(
    text: String,
    peakPosition: () -> Float?,
    glyphXPositions: FloatArray,
    glyphBaselines: FloatArray,
    glyphPeakPositions: FloatArray?,
    horizontalRevealPosition: (() -> Float?)?,
    bloom: (() -> LyricsBloom?)?,
    unrevealedAlpha: () -> Float,
    offsetX: Float,
    offsetY: Float,
    color: Color,
    fontSize: TextUnit,
    uniformWeight: (() -> Float)?,
    uniformWidth: (() -> Float)?,
    fontScale: () -> Float,
    scaleFromBottom: Boolean,
    uniformLines: (() -> List<String>)?,
    lineHeight: TextUnit,
    modifier: Modifier,
) {
    val fontFamily = FontFamily(
        Font(Res.font.GoogleSansFlex, weight = FontWeight.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(400), FontVariation.Setting("ROND", 100f))),
        Font(Res.font.GoogleSansFlex, weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(FontVariation.weight(700), FontVariation.Setting("ROND", 100f))),
        Font(Res.font.GoogleSansFlex, weight = FontWeight.Black,
            variationSettings = FontVariation.Settings(FontVariation.weight(900), FontVariation.Setting("ROND", 100f))),
    )
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val baseStyle = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal,
        fontSize = fontSize, lineHeight = lineHeight)
    val heavyStyle = baseStyle.copy(fontWeight = FontWeight.Bold)
    val lines = remember(text, glyphXPositions, glyphBaselines) {
        buildList {
            var start = 0
            while (start < text.length) {
                val baseline = glyphBaselines.getOrElse(start) { 0f }
                var end = start + 1
                while (end < text.length && glyphBaselines.getOrElse(end) { baseline } == baseline) end++
                add(ShapedLine(start, end, baseline,
                    (start until end).minOfOrNull { glyphXPositions.getOrElse(it) { 0f } } ?: 0f))
                start = end
            }
        }
    }
    Canvas(modifier) {
        val resolvedScale = fontScale().coerceAtLeast(0f)
        if (uniformWeight != null || uniformWidth != null) {
            val weight = (uniformWeight?.invoke() ?: 400f).toInt().coerceIn(1, 1000)
            val width = (uniformWidth?.invoke() ?: 100f).coerceIn(25f, 200f) / 100f
            val renderedLines = uniformLines?.invoke() ?: listOf(text)
            val blend = ((weight - 400f) / 500f).coerceIn(0f, 1f)
            val baseline = glyphBaselines.firstOrNull() ?: 0f
            val origin = Offset(offsetX + (glyphXPositions.firstOrNull() ?: 0f),
                if (scaleFromBottom) size.height - (size.height - offsetY - baseline) * resolvedScale
                else offsetY + baseline * resolvedScale)
            var y = 0f
            for (line in renderedLines) {
                val layout = measurer.measure(line, baseStyle, softWrap = false,
                    constraints = Constraints(maxWidth = Constraints.Infinity))
                val bold = measurer.measure(line, baseStyle.copy(fontWeight = FontWeight.Black), softWrap = false,
                    constraints = Constraints(maxWidth = Constraints.Infinity))
                scale(width * resolvedScale, resolvedScale, pivot = origin) {
                    drawText(layout, color, topLeft = Offset(origin.x, origin.y - layout.firstBaseline + y))
                    scale(layout.size.width.toFloat() / bold.size.width.coerceAtLeast(1), 1f, pivot = origin) {
                        drawText(bold, color, topLeft = Offset(origin.x, origin.y - bold.firstBaseline + y), alpha = blend)
                    }
                }
                y += if (lineHeight.isSp) lineHeight.toPx() else layout.size.height.toFloat()
            }
            return@Canvas
        }
        val peak = peakPosition()
        val reveal = horizontalRevealPosition?.invoke()
        val halo = bloom?.invoke()
        for ((start, end1, baseline, x) in lines) {
            val value = text.substring(start, end1)
            val base = measurer.measure(value, baseStyle, softWrap = false,
                constraints = Constraints(maxWidth = Constraints.Infinity))
            val heavy = measurer.measure(value, heavyStyle, softWrap = false,
                constraints = Constraints(maxWidth = Constraints.Infinity))
            val origin = Offset(offsetX + x, offsetY + baseline - base.firstBaseline)
            val alpha = if (reveal != null) unrevealedAlpha().coerceIn(0f, 1f) else 1f
            drawText(base, color, topLeft = origin, alpha = alpha)
            if (reveal != null) {
                val visible = (reveal - start).coerceIn(0f, value.length.toFloat())
                val end = visible.toInt()
                if (end > 0) {
                    val path = base.getPathForRange(0, end).apply { translate(origin) }
                    clipPath(path) { drawText(base, color, topLeft = origin) }
                }
                if (end < value.length && visible > end) {
                    val box = base.getBoundingBox(end)
                    val fraction = visible - end
                    val rtl = base.getBidiRunDirection(end) == androidx.compose.ui.text.style.ResolvedTextDirection.Rtl
                    val left = if (rtl) box.right - box.width * fraction else box.left
                    val right = if (rtl) box.right else box.left + box.width * fraction
                    clipRect(
                        left = origin.x + left, top = origin.y + box.top,
                        right = origin.x + right, bottom = origin.y + box.bottom,
                    ) { drawText(base, color, topLeft = origin) }
                }
            }
            if (halo != null && halo.strength > 0f) {
                val from = (halo.startPosition - start).coerceIn(0, value.length)
                val to = (halo.endPositionExclusive - start).coerceIn(from, value.length)
                if (to > from) {
                    val bounds = base.getPathForRange(from, to).getBounds()
                    val radius = baseStyle.fontSize.toPx() * 0.18f * halo.strength
                    clipRect(origin.x + bounds.left - radius, origin.y + bounds.top - radius,
                        origin.x + bounds.right + radius, origin.y + bounds.bottom + radius) {
                        drawText(base, color, topLeft = origin, alpha = halo.strength * 0.4f,
                            shadow = Shadow(color, blurRadius = radius))
                    }
                }
            }
            if (peak == null) continue
            // Blend fully shaped runs through word masks. Changing an animation
            // mask never changes Unicode segmentation or tears apart a ligature.
            var cursor = 0
            while (cursor < value.length) {
                val range = base.getWordBoundary(cursor)
                val end = range.end.coerceAtLeast(cursor + 1).coerceAtMost(value.length)
                val center = start + (cursor + end - 1) / 2
                val position = glyphPeakPositions?.getOrNull(center) ?: center.toFloat()
                val influence = lyricsWeightProgress(quantizeLyricsWeight(
                    lyricsWeightAt(position, peak, text.lastIndex.toFloat())))
                if (influence > 0f && value.substring(cursor, end).isNotBlank()) {
                    val path = base.getPathForRange(cursor, end).apply { translate(origin) }
                    val revealedAlpha = if (reveal == null || reveal >= start + end) 1f else alpha
                    clipPath(path) {
                        scale(base.size.width.toFloat() / heavy.size.width.coerceAtLeast(1), 1f, pivot = origin) {
                            drawText(heavy, color, topLeft = Offset(origin.x,
                                origin.y + base.firstBaseline - heavy.firstBaseline), alpha = influence * revealedAlpha)
                        }
                    }
                }
                cursor = end
            }
        }
    }
}
