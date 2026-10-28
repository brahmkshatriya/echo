package dev.brahmkshatriya.echo.app.ui.player.song.lyrics

import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.ui.theme.googleSansFontFamily
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

internal val FullLyricsLinePadding = 8.dp
internal val FullLyricsAuxiliaryGap = 4.dp
internal val FullLyricsPositionGroupGap = 14.dp

internal class FullLyricsLineLayout(val textLayout: TextLayoutResult, val heightPx: Int)

/** Resolve wrapping once, before allowing scrolling, without composing every lyric row. */
@Composable
internal fun rememberFullLyricsLineLayouts(
    timingIndex: LyricsTimingIndex,
    widthPx: Int,
): List<FullLyricsLineLayout>? {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val fontFamily = googleSansFontFamily()
    val wordStyle = typography.headlineMedium.copy(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontFeatureSettings = "liga 0, kern 1",
        lineHeight = typography.headlineLarge.lineHeight,
    )
    val lineStyle = typography.headlineMedium.copy(
        fontWeight = FontWeight.Normal,
        lineHeight = typography.headlineLarge.lineHeight,
    )
    val translationStyle = typography.bodyLarge.copy(fontWeight = FontWeight.Normal)
    val vocalsStyle = typography.bodyMedium.copy(fontWeight = FontWeight.Normal)
    val paddingPx = with(density) { FullLyricsLinePadding.roundToPx() }
    val gapPx = with(density) { FullLyricsAuxiliaryGap.roundToPx() }
    return key(timingIndex, widthPx, measurer, wordStyle, lineStyle, translationStyle, vocalsStyle) {
        val layouts by produceState<List<FullLyricsLineLayout>?>(null) {
            if (widthPx <= 0) return@produceState
            val constraints = Constraints.fixedWidth((widthPx - paddingPx * 2).coerceAtLeast(0))
            val result = ArrayList<FullLyricsLineLayout>(timingIndex.lines.size)
            var slice = TimeSource.Monotonic.markNow()
            for (timing in timingIndex.lines) {
                val align = timing.line.position.textAlign
                val text = measurer.measure(
                    text = timing.text,
                    style = (if (timing is WordLineTiming) wordStyle else lineStyle).copy(textAlign = align),
                    constraints = constraints,
                )
                var height = text.size.height + paddingPx * 2
                val vocals = (timing.line as? WordsLyric)?.backgroundVocals.orEmpty()
                if (vocals.isNotEmpty()) {
                    height += gapPx + measurer.measure(
                        text = vocals.joinToString("") { it.text + it.trailingSpace },
                        style = vocalsStyle.copy(textAlign = align),
                        constraints = constraints,
                    ).size.height
                }
                val translations = when (val line = timing.line) {
                    is WordsLyric -> line.translations
                    is LineLyric -> line.translations
                }
                for ((_, text1) in translations) {
                    height += gapPx + measurer.measure(
                        text = text1,
                        style = translationStyle.copy(textAlign = align),
                        constraints = constraints,
                    ).size.height
                }
                result.add(FullLyricsLineLayout(text, height))
                // Long tracks must not turn their first layout into one long UI frame.
                if (slice.elapsedNow() >= 4.milliseconds) {
                    withFrameNanos { }
                    slice = TimeSource.Monotonic.markNow()
                }
            }
            value = result
        }
        layouts
    }
}
