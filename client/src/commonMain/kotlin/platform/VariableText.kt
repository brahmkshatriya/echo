package dev.brahmkshatriya.echo.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val LyricsMinWeight = 400f
internal const val LyricsMaxWeight = 700f
internal const val LyricsWeightRange = LyricsMaxWeight - LyricsMinWeight
internal const val LyricsWeightStep = 16
internal const val LyricsMinAlpha = 0.66f
internal const val LyricsMaxRoundness = 100f

internal data class LyricsBloom(
    val startPosition: Int,
    val endPositionExclusive: Int,
    val strength: Float,
)

internal fun lyricsWeightAt(
    position: Float,
    peakPosition: Float,
    lastPosition: Float,
): Float {
    if (lastPosition <= 0f) return LyricsMaxWeight

    val peak = peakPosition.coerceAtLeast(0f)
    val influenceRadius = lastPosition / 4f
    val distanceFromPeak = abs(position - peak)
    val progress = 1f - distanceFromPeak / influenceRadius
    return LyricsMinWeight + LyricsWeightRange * progress.coerceIn(0f, 1f)
}

internal fun quantizeLyricsWeight(weight: Float): Int {
    return ((weight / LyricsWeightStep).roundToInt() * LyricsWeightStep)
        .coerceIn(LyricsMinWeight.toInt(), LyricsMaxWeight.toInt())
}

internal fun lyricsWeightProgress(quantizedWeight: Int): Float {
    return ((quantizedWeight - LyricsMinWeight) / LyricsWeightRange)
        .coerceIn(0f, 1f)
}

internal fun lyricsAlphaAt(
    position: Float,
    peakPosition: Float,
    quantizedWeight: Int,
): Float {
    if (position <= peakPosition) return 1f

    val weightProgress = lyricsWeightProgress(quantizedWeight)
    return LyricsMinAlpha + (1f - LyricsMinAlpha) * weightProgress
}

internal fun lyricsRoundnessAt(
    position: Float,
    peakPosition: Float,
    quantizedWeight: Int,
): Float {
    if (position <= peakPosition) return LyricsMaxRoundness

    return LyricsMaxRoundness * lyricsWeightProgress(quantizedWeight)
}

@Suppress("KotlinNoActualForExpect")
@Composable
internal expect fun VariableText(
    text: String,
    peakPosition: () -> Float?,
    glyphXPositions: FloatArray,
    glyphBaselines: FloatArray,
    glyphPeakPositions: FloatArray? = null,
    glyphRightPositions: FloatArray? = null,
    horizontalRevealPosition: (() -> Float?)? = null,
    bloom: (() -> LyricsBloom?)? = null,
    horizontalRevealFeather: Float = 0.45f,
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
    modifier: Modifier = Modifier,
)
