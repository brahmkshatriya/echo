package dev.brahmkshatriya.echo.app.platform

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import echo.app.generated.resources.GoogleSansFlex
import echo.app.generated.resources.Res
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.rememberResourceEnvironment
import org.jetbrains.skia.Data
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontEdging
import org.jetbrains.skia.FontHinting
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontVariation
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Typeface
import kotlin.math.roundToInt

private var variableLyricsBaseTypeface by mutableStateOf<Typeface?>(null)
private var variableLyricsTypefaceLoading by mutableStateOf(false)

@Composable
internal actual fun VariableText(
    text: String,
    peakPosition: Float?,
    glyphXPositions: FloatArray,
    glyphBaselines: FloatArray,
    offsetX: Float,
    offsetY: Float,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier,
) {
    val environment = rememberResourceEnvironment()
    LaunchedEffect(environment) {
        if (variableLyricsBaseTypeface == null && !variableLyricsTypefaceLoading) {
            variableLyricsTypefaceLoading = true
            try {
                val bytes = getFontResourceBytes(environment, Res.font.GoogleSansFlex)
                variableLyricsBaseTypeface = FontMgr.default.makeFromData(Data.makeFromBytes(bytes))
            } finally {
                variableLyricsTypefaceLoading = false
            }
        }
    }

    val density = LocalDensity.current
    val fontSizePx = with(density) {
        if (fontSize.isSp) fontSize.toPx() else 16.sp.toPx()
    }
    val glyphTexts = remember(text) { text.map { it.toString() } }
    val baseTypeface = variableLyricsBaseTypeface
    val fontCache = remember(baseTypeface, fontSizePx) {
        baseTypeface?.let { VariableLyricsSkiaFontCache(it, fontSizePx) }
    }
    DisposableEffect(fontCache) {
        onDispose { fontCache?.close() }
    }

    Canvas(modifier) {
        val cache = fontCache ?: return@Canvas
        if (glyphTexts.isEmpty()) return@Canvas

        val lastPosition = glyphTexts.lastIndex.toFloat()
        cache.paint.color = color.toArgb()
        glyphTexts.forEachIndexed { index, glyph ->
            val font = cache.fontFor(
                position = index.toFloat(),
                peakPosition = peakPosition,
                lastPosition = lastPosition,
            )
            val alpha = if (peakPosition == null) {
                1f
            } else {
                lyricsAlphaAt(
                    position = index.toFloat(),
                    peakPosition = peakPosition,
                    quantizedWeight = font.weight,
                )
            }
            cache.paint.setAlphaf(alpha)
            drawContext.canvas.skiaCanvas.drawString(
                glyph,
                offsetX + glyphXPositions.getOrElse(index) { 0f },
                offsetY + glyphBaselines.getOrElse(index) { 0f },
                font.font,
                cache.paint,
            )
        }
    }
}

private data class VariableLyricsSkiaFontKey(
    val weight: Int,
    val roundness: Int,
)

private class VariableLyricsSkiaFontCache(
    private val baseTypeface: Typeface,
    private val fontSize: Float,
) {
    private val fonts = mutableMapOf<VariableLyricsSkiaFontKey, VariableLyricsSkiaFont>()
    val paint = Paint().apply { isAntiAlias = true }

    fun fontFor(
        position: Float,
        peakPosition: Float?,
        lastPosition: Float,
    ): VariableLyricsSkiaFont {
        if (peakPosition == null) {
            return fontFor(
                weight = LyricsMinWeight.toInt(),
                roundness = LyricsMaxRoundness.toInt(),
            )
        }

        val weight = quantizeLyricsWeight(
            lyricsWeightAt(position, peakPosition, lastPosition)
        )
        val roundness = lyricsRoundnessAt(position, peakPosition, weight)
            .roundToInt()
            .coerceIn(0, LyricsMaxRoundness.toInt())
        return fontFor(weight, roundness)
    }

    private fun fontFor(weight: Int, roundness: Int): VariableLyricsSkiaFont {
        val key = VariableLyricsSkiaFontKey(weight, roundness)
        return fonts.getOrPut(key) {
            VariableLyricsSkiaFont(baseTypeface, key, fontSize)
        }
    }

    fun close() {
        fonts.values.forEach(VariableLyricsSkiaFont::close)
        fonts.clear()
        paint.close()
    }
}

private class VariableLyricsSkiaFont(
    baseTypeface: Typeface,
    key: VariableLyricsSkiaFontKey,
    fontSize: Float,
) {
    val weight = key.weight
    private val typeface = baseTypeface.makeClone(
        arrayOf(
            FontVariation("wght", key.weight.toFloat()),
            FontVariation("ROND", key.roundness.toFloat()),
        )
    )
    val font = Font(typeface, fontSize).apply {
        isAutoHintingForced = false
        isSubpixel = true
        isLinearMetrics = true
        isBaselineSnapped = false
        hinting = FontHinting.NONE
        edging = FontEdging.ANTI_ALIAS
    }

    fun close() {
        font.close()
        typeface.close()
    }
}
