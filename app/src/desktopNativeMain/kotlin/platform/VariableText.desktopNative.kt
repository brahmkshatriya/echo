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
private val variableLyricsTypefaces = mutableMapOf<Int, Typeface>()
private val variableTextUniformTypefaces = mutableMapOf<Int, Typeface>()

private fun variableLyricsFontKey(weight: Int, roundness: Int): Int =
    (weight shl 8) or roundness

private fun variableTextUniformFontKey(weight: Int, width: Int): Int =
    (weight shl 9) or width

private suspend fun ensureVariableTextBaseTypeface(
    environment: org.jetbrains.compose.resources.ResourceEnvironment,
): Typeface {
    variableLyricsBaseTypeface?.let { return it }
    if (variableLyricsTypefaceLoading) {
        while (variableLyricsTypefaceLoading) kotlinx.coroutines.yield()
        return requireNotNull(variableLyricsBaseTypeface)
    }

    variableLyricsTypefaceLoading = true
    return try {
        val bytes = getFontResourceBytes(environment, Res.font.GoogleSansFlex)
        requireNotNull(FontMgr.default.makeFromData(Data.makeFromBytes(bytes)))
            .also { variableLyricsBaseTypeface = it }
    } finally {
        variableLyricsTypefaceLoading = false
    }
}

@Composable
internal actual fun VariableText(
    text: String,
    peakPosition: () -> Float?,
    glyphXPositions: FloatArray,
    glyphBaselines: FloatArray,
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
    val environment = rememberResourceEnvironment()
    LaunchedEffect(environment) {
        ensureVariableTextBaseTypeface(environment)
    }

    val density = LocalDensity.current
    val fontSizePx = with(density) {
        if (fontSize.isSp) fontSize.toPx() else 16.sp.toPx()
    }
    val lineHeightPx = with(density) {
        if (lineHeight.isSp) lineHeight.toPx() else fontSizePx
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
        val resolvedScale = fontScale().coerceAtLeast(0f)
        cache.paint.color = color.toArgb()

        if (uniformWeight != null || uniformWidth != null) {
            val weight = (((uniformWeight?.invoke() ?: 400f) / 8f).roundToInt() * 8)
                .coerceIn(1, 1000)
            val width = (uniformWidth?.invoke() ?: 100f).roundToInt().coerceIn(25, 200)
            val font = cache.uniformFontFor(weight, width).font
            font.size = fontSizePx * resolvedScale
            cache.paint.setAlphaf(1f)
            val baseline = glyphBaselines.firstOrNull() ?: 0f
            val baselineY = if (scaleFromBottom) {
                size.height - (size.height - offsetY - baseline) * resolvedScale
            } else {
                offsetY + baseline * resolvedScale
            }
            val firstX = glyphXPositions.firstOrNull() ?: 0f
            val drawX = offsetX + firstX
            val lines = uniformLines?.invoke() ?: listOf(text)
            lines.forEachIndexed { index, line ->
                drawContext.canvas.skiaCanvas.drawString(
                    line,
                    drawX,
                    baselineY + index * lineHeightPx * resolvedScale,
                    font,
                    cache.paint,
                )
            }
            return@Canvas
        }

        val peakPosition = peakPosition()
        if (peakPosition == null) {
            val font = cache.staticFont.font
            cache.paint.setAlphaf(1f)
            glyphTexts.forEachIndexed { index, glyph ->
                drawContext.canvas.skiaCanvas.drawString(
                    glyph,
                    offsetX + glyphXPositions.getOrElse(index) { 0f },
                    offsetY + glyphBaselines.getOrElse(index) { 0f },
                    font,
                    cache.paint,
                )
            }
            return@Canvas
        }

        glyphTexts.forEachIndexed { index, glyph ->
            val position = index.toFloat()
            val font = cache.fontFor(
                position = position,
                peakPosition = peakPosition,
                lastPosition = lastPosition,
            )
            cache.paint.setAlphaf(
                lyricsAlphaAt(
                    position = position,
                    peakPosition = peakPosition,
                    quantizedWeight = font.weight,
                )
            )
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

private class VariableLyricsSkiaFontCache(
    private val baseTypeface: Typeface,
    private val fontSize: Float,
) {
    private val fonts = mutableMapOf<Int, VariableLyricsSkiaFont>()
    private val uniformFonts = mutableMapOf<Int, VariableLyricsSkiaFont>()
    val paint = Paint().apply { isAntiAlias = true }
    val staticFont by lazy(LazyThreadSafetyMode.NONE) {
        fontFor(
            weight = LyricsMinWeight.toInt(),
            roundness = LyricsMaxRoundness.toInt(),
        )
    }

    fun uniformFontFor(weight: Int, width: Int): VariableLyricsSkiaFont {
        val key = variableTextUniformFontKey(weight, width)
        return uniformFonts.getOrPut(key) {
            VariableLyricsSkiaFont(
                typeface = variableTextUniformTypeface(
                    baseTypeface = baseTypeface,
                    key = key,
                    weight = weight,
                    width = width,
                ),
                weight = weight,
                fontSize = fontSize,
            )
        }
    }

    fun fontFor(
        position: Float,
        peakPosition: Float,
        lastPosition: Float,
    ): VariableLyricsSkiaFont {
        val weight = quantizeLyricsWeight(
            lyricsWeightAt(position, peakPosition, lastPosition)
        )
        val roundness = lyricsRoundnessAt(position, peakPosition, weight)
            .roundToInt()
            .coerceIn(0, LyricsMaxRoundness.toInt())
        return fontFor(weight, roundness)
    }

    private fun fontFor(weight: Int, roundness: Int): VariableLyricsSkiaFont {
        val key = variableLyricsFontKey(weight, roundness)
        return fonts.getOrPut(key) {
            VariableLyricsSkiaFont(
                typeface = variableLyricsTypeface(
                    baseTypeface = baseTypeface,
                    key = key,
                    weight = weight,
                    roundness = roundness,
                ),
                weight = weight,
                fontSize = fontSize,
            )
        }
    }

    fun close() {
        fonts.values.forEach(VariableLyricsSkiaFont::close)
        uniformFonts.values.forEach(VariableLyricsSkiaFont::close)
        fonts.clear()
        uniformFonts.clear()
        paint.close()
    }
}

private fun variableLyricsTypeface(
    baseTypeface: Typeface,
    key: Int,
    weight: Int,
    roundness: Int,
): Typeface = variableLyricsTypefaces.getOrPut(key) {
    baseTypeface.makeClone(
        arrayOf(
            FontVariation("wght", weight.toFloat()),
            FontVariation("ROND", roundness.toFloat()),
        )
    )
}

private fun variableTextUniformTypeface(
    baseTypeface: Typeface,
    key: Int,
    weight: Int,
    width: Int,
): Typeface = variableTextUniformTypefaces.getOrPut(key) {
    baseTypeface.makeClone(
        arrayOf(
            FontVariation("wght", weight.toFloat()),
            FontVariation("wdth", width.toFloat()),
        )
    )
}

private class VariableLyricsSkiaFont(
    typeface: Typeface,
    val weight: Int,
    fontSize: Float,
) {
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
    }
}
