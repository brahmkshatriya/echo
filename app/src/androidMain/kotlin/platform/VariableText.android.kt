package dev.brahmkshatriya.echo.app.platform

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.fonts.FontVariationAxis
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import android.graphics.fonts.Font as AndroidFont
import android.graphics.fonts.FontFamily as AndroidFontFamily

private const val GoogleSansFlexAssetPath =
    "composeResources/echo.app.generated.resources/font/GoogleSansFlex.ttf"

private var variableLyricsAndroidBaseTypeface: Typeface? = null
private var variableLyricsAndroidBaseFont: AndroidFont? = null
private val variableLyricsAndroidTypefaces = mutableMapOf<Int, Typeface>()

private fun variableLyricsFontKey(weight: Int, roundness: Int): Int =
    (weight shl 8) or roundness

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
    modifier: Modifier,
) {
    val assets = LocalContext.current.assets
    val density = LocalDensity.current
    val fontSizePx = with(density) {
        if (fontSize.isSp) fontSize.toPx() else 16.sp.toPx()
    }
    val glyphTexts = remember(text) { text.map { it.toString() } }
    val cache = remember(assets, fontSizePx) {
        VariableLyricsAndroidFontCache(assets, fontSizePx)
    }

    Canvas(modifier) {
        if (glyphTexts.isEmpty()) return@Canvas

        val lastPosition = glyphTexts.lastIndex.toFloat()
        val peakPosition = peakPosition()
        cache.drawPaint.color = color.toArgb()
        cache.drawPaint.textSize = fontSizePx
        if (peakPosition == null) {
            cache.drawPaint.typeface = cache.staticFont.typeface
            cache.drawPaint.alpha = 255
            glyphTexts.forEachIndexed { index, glyph ->
                drawContext.canvas.nativeCanvas.drawText(
                    glyph,
                    offsetX + glyphXPositions.getOrElse(index) { 0f },
                    offsetY + glyphBaselines.getOrElse(index) { 0f },
                    cache.drawPaint,
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
            cache.drawPaint.typeface = font.typeface
            val alpha = lyricsAlphaAt(
                position = position,
                peakPosition = peakPosition,
                quantizedWeight = font.weight,
            )
            cache.drawPaint.alpha = (alpha * 255f).roundToInt().coerceIn(0, 255)
            drawContext.canvas.nativeCanvas.drawText(
                glyph,
                offsetX + glyphXPositions.getOrElse(index) { 0f },
                offsetY + glyphBaselines.getOrElse(index) { 0f },
                cache.drawPaint,
            )
        }
    }
}

private class VariableLyricsAndroidFontCache(
    private val assets: android.content.res.AssetManager,
    fontSize: Float,
) {
    private val fonts = mutableMapOf<Int, VariableLyricsAndroidFont>()
    val staticFont by lazy(LazyThreadSafetyMode.NONE) {
        fontFor(
            weight = LyricsMinWeight.toInt(),
            roundness = LyricsMaxRoundness.toInt(),
        )
    }
    val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = fontSize
        isSubpixelText = true
        isLinearText = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            hinting = Paint.HINTING_OFF
        }
    }

    fun fontFor(
        position: Float,
        peakPosition: Float,
        lastPosition: Float,
    ): VariableLyricsAndroidFont {
        val weight = quantizeLyricsWeight(
            lyricsWeightAt(position, peakPosition, lastPosition)
        )
        val roundness = lyricsRoundnessAt(position, peakPosition, weight)
            .roundToInt()
            .coerceIn(0, LyricsMaxRoundness.toInt())
        return fontFor(weight, roundness)
    }

    private fun fontFor(weight: Int, roundness: Int): VariableLyricsAndroidFont {
        val key = variableLyricsFontKey(weight, roundness)
        return fonts.getOrPut(key) {
            VariableLyricsAndroidFont(
                weight = weight,
                typeface = variableLyricsAndroidTypeface(
                    assets = assets,
                    key = key,
                    weight = weight,
                    roundness = roundness,
                ),
            )
        }
    }
}

private data class VariableLyricsAndroidFont(
    val weight: Int,
    val typeface: Typeface,
)

private fun variableLyricsAndroidTypeface(
    assets: android.content.res.AssetManager,
    key: Int,
    weight: Int,
    roundness: Int,
): Typeface {
    return variableLyricsAndroidTypefaces.getOrPut(key) {
        when {
            Build.VERSION.SDK_INT >= 31 -> {
                val baseFont = variableLyricsAndroidBaseFont ?: AndroidFont.Builder(
                    assets,
                    GoogleSansFlexAssetPath,
                ).build().also { variableLyricsAndroidBaseFont = it }
                val variableFont = AndroidFont.Builder(baseFont)
                    .setWeight(weight)
                    .setFontVariationSettings(
                        arrayOf(
                            FontVariationAxis("wght", weight.toFloat()),
                            FontVariationAxis("ROND", roundness.toFloat()),
                        )
                    )
                    .build()
                val family = AndroidFontFamily.Builder(variableFont).build()
                Typeface.CustomFallbackBuilder(family).build()
            }

            Build.VERSION.SDK_INT >= 28 -> {
                val baseTypeface = variableLyricsAndroidBaseTypeface
                    ?: Typeface.createFromAsset(assets, GoogleSansFlexAssetPath)
                        .also { variableLyricsAndroidBaseTypeface = it }
                Typeface.create(baseTypeface, weight, false)
            }

            else -> {
                variableLyricsAndroidBaseTypeface
                    ?: Typeface.createFromAsset(assets, GoogleSansFlexAssetPath)
                        .also { variableLyricsAndroidBaseTypeface = it }
            }
        }
    }
}
