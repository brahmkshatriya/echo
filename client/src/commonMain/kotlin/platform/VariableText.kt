package dev.brahmkshatriya.echo.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import kotlin.math.abs
import kotlin.math.roundToInt

const val LyricsMinWeight = 400f
const val LyricsMaxWeight = 700f
const val LyricsWeightRange = LyricsMaxWeight - LyricsMinWeight
const val LyricsWeightStep = 16
const val LyricsMinAlpha = 0.66f
const val LyricsMaxRoundness = 100f

/** These precomposed Latin characters and punctuation have independent glyphs in our font. */
fun requiresShapedVariableText(text: String): Boolean =
    text.any {
        when (it.code) {
            in 0x00..0x7f,
            in 0x00c0..0x00ff,
            in 0x2013..0x2014,
            in 0x2018..0x201a,
            in 0x201c..0x201e -> false
            // Keep combining marks, joining scripts, bidi text and emoji on the full-run renderer.
            else -> true
        }
    }

data class LyricsBloom(val startPosition: Int, val endPositionExclusive: Int, val strength: Float)

fun lyricsWeightAt(position: Float, peakPosition: Float, lastPosition: Float): Float {
    if (lastPosition <= 0f) return LyricsMaxWeight

    val peak = peakPosition.coerceAtLeast(0f)
    val influenceRadius = lastPosition / 4f
    val distanceFromPeak = abs(position - peak)
    val progress = 1f - distanceFromPeak / influenceRadius
    return LyricsMinWeight + LyricsWeightRange * progress.coerceIn(0f, 1f)
}

fun quantizeLyricsWeight(weight: Float): Int =
    ((weight / LyricsWeightStep).roundToInt() * LyricsWeightStep).coerceIn(
        LyricsMinWeight.toInt(),
        LyricsMaxWeight.toInt(),
    )

fun lyricsWeightProgress(quantizedWeight: Int): Float =
    ((quantizedWeight - LyricsMinWeight) / LyricsWeightRange).coerceIn(0f, 1f)

fun lyricsAlphaAt(position: Float, peakPosition: Float, quantizedWeight: Int): Float {
    if (position <= peakPosition) return 1f

    val weightProgress = lyricsWeightProgress(quantizedWeight)
    return LyricsMinAlpha + (1f - LyricsMinAlpha) * weightProgress
}

fun lyricsRoundnessAt(position: Float, peakPosition: Float, quantizedWeight: Int): Float {
    if (position <= peakPosition) return LyricsMaxRoundness

    return LyricsMaxRoundness * lyricsWeightProgress(quantizedWeight)
}

@Composable
expect fun VariableText(
    modifier: Modifier = Modifier,
    text: String,
    peakPosition: () -> Float?,
    glyphXPositions: FloatArray,
    glyphBaselines: FloatArray,
    glyphPeakPositions: FloatArray? = null,
    glyphRightPositions: FloatArray? = null,
    horizontalRevealPosition: (() -> Float?)? = null,
    bloom: (() -> LyricsBloom?)? = null,
    horizontalRevealFeather: Float = 0.45f,
    unrevealedAlpha: () -> Float = { LyricsMinAlpha },
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    color: Color,
    fontSize: TextUnit,
    uniformWeight: (() -> Float)? = null,
    uniformWidth: (() -> Float)? = null,
    fontScale: () -> Float = { 1f },
    scaleFromBottom: Boolean = false,
    uniformLines: (() -> List<String>)? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
)
