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
private val variableLyricsAndroidTypefaces =
    mutableMapOf<VariableLyricsAndroidTypefaceKey, Typeface>()

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
        cache.drawPaint.color = color.toArgb()
        cache.drawPaint.textSize = fontSizePx
        glyphTexts.forEachIndexed { index, glyph ->
            val font = cache.fontFor(
                position = index.toFloat(),
                peakPosition = peakPosition,
                lastPosition = lastPosition,
            )
            cache.drawPaint.typeface = font.typeface
            val alpha = if (peakPosition == null) {
                1f
            } else {
                lyricsAlphaAt(
                    position = index.toFloat(),
                    peakPosition = peakPosition,
                    quantizedWeight = font.weight,
                )
            }
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

private data class VariableLyricsAndroidTypefaceKey(
    val weight: Int,
    val roundness: Int,
)

private class VariableLyricsAndroidFontCache(
    private val assets: android.content.res.AssetManager,
    fontSize: Float,
) {
    private val fonts = mutableMapOf<VariableLyricsAndroidTypefaceKey, VariableLyricsAndroidFont>()
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
        peakPosition: Float?,
        lastPosition: Float,
    ): VariableLyricsAndroidFont {
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

    private fun fontFor(weight: Int, roundness: Int): VariableLyricsAndroidFont {
        val key = VariableLyricsAndroidTypefaceKey(weight, roundness)
        return fonts.getOrPut(key) {
            VariableLyricsAndroidFont(
                weight = weight,
                typeface = variableLyricsAndroidTypeface(assets, key),
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
    key: VariableLyricsAndroidTypefaceKey,
): Typeface {
    return variableLyricsAndroidTypefaces.getOrPut(key) {
        when {
            Build.VERSION.SDK_INT >= 31 -> {
                val baseFont = variableLyricsAndroidBaseFont ?: AndroidFont.Builder(
                    assets,
                    GoogleSansFlexAssetPath,
                ).build().also { variableLyricsAndroidBaseFont = it }
                val variableFont = AndroidFont.Builder(baseFont)
                    .setWeight(key.weight)
                    .setFontVariationSettings(
                        arrayOf(
                            FontVariationAxis("wght", key.weight.toFloat()),
                            FontVariationAxis("ROND", key.roundness.toFloat()),
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
                Typeface.create(baseTypeface, key.weight, false)
            }

            else -> {
                variableLyricsAndroidBaseTypeface
                    ?: Typeface.createFromAsset(assets, GoogleSansFlexAssetPath)
                        .also { variableLyricsAndroidBaseTypeface = it }
            }
        }
    }
}
