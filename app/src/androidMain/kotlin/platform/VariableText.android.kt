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
private val variableTextAndroidUniformTypefaces = mutableMapOf<Int, Typeface>()

private fun variableLyricsFontKey(weight: Int, roundness: Int): Int =
    (weight shl 8) or roundness

private fun variableTextUniformFontKey(weight: Int, width: Int): Int =
    (weight shl 9) or width

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
    val assets = LocalContext.current.assets
    val density = LocalDensity.current
    val fontSizePx = with(density) {
        if (fontSize.isSp) fontSize.toPx() else 16.sp.toPx()
    }
    val lineHeightPx = with(density) {
        if (lineHeight.isSp) lineHeight.toPx() else fontSizePx
    }
    val glyphTexts = remember(text) { text.map { it.toString() } }
    val cache = remember(assets, fontSizePx) {
        VariableLyricsAndroidFontCache(assets, fontSizePx)
    }

    Canvas(modifier) {
        if (glyphTexts.isEmpty()) return@Canvas

        val lastPosition = glyphTexts.lastIndex.toFloat()
        val resolvedScale = fontScale().coerceAtLeast(0f)
        cache.drawPaint.color = color.toArgb()
        cache.drawPaint.textSize = fontSizePx * resolvedScale
        cache.drawPaint.textScaleX = 1f

        if (uniformWeight != null || uniformWidth != null) {
            val weight = (((uniformWeight?.invoke() ?: 400f) / 8f).roundToInt() * 8)
                .coerceIn(1, 1000)
            val width = (uniformWidth?.invoke() ?: 100f).roundToInt().coerceIn(25, 200)
            cache.drawPaint.typeface = cache.uniformTypeface(weight, width)
            if (Build.VERSION.SDK_INT < 31) {
                cache.drawPaint.textScaleX = width / 100f
            }
            cache.drawPaint.alpha = 255
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
                drawContext.canvas.nativeCanvas.drawText(
                    line,
                    drawX,
                    baselineY + index * lineHeightPx * resolvedScale,
                    cache.drawPaint,
                )
            }
            return@Canvas
        }

        val peakPosition = peakPosition()
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

    fun uniformTypeface(weight: Int, width: Int): Typeface =
        variableTextAndroidUniformTypeface(
            assets = assets,
            key = variableTextUniformFontKey(weight, width),
            weight = weight,
            width = width,
        )

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

private fun variableTextAndroidUniformTypeface(
    assets: android.content.res.AssetManager,
    key: Int,
    weight: Int,
    width: Int,
): Typeface = variableTextAndroidUniformTypefaces.getOrPut(key) {
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
                        FontVariationAxis("wdth", width.toFloat()),
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
