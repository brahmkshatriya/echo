package dev.brahmkshatriya.echo.app.ui.player.song.lyrics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.brahmkshatriya.echo.app.playback.LocalPlayerTimelineState
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerControls
import kotlin.math.min

private const val WordStretchMinLength = 6
private const val WordStretchMinDurationMs = 750L
private const val MotionTransitionFraction = 0.35f
private const val LineLeadInMs = 160L
private const val LineTailMs = 260L
// Text shadows extend beyond glyph/layout bounds. Leave room *inside* the
// graphics layer so alpha animation cannot crop the blur against its edges.
private val MotionLyricShadowInset = 26.dp

private fun smoothstep(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Give quick lyrics several transition frames while retaining a fully readable hold. */
private fun motionTransitionMs(durationMs: Long, maxMs: Float): Float =
    min(maxMs, durationMs.coerceAtLeast(1L) * MotionTransitionFraction).coerceAtLeast(1f)

/** Sustained, long words widen only while sung. Short/quick words stay at natural width. */
internal fun motionWordStretch(token: TimedToken, positionMs: Long): Float {
    val letters = token.text.count { it.isLetterOrDigit() }
    val duration = token.endMs - token.startMs
    if (letters < WordStretchMinLength || duration <= WordStretchMinDurationMs ||
        positionMs !in token.startMs..token.endMs) return 1f

    val durationStretch = ((duration - WordStretchMinDurationMs) / 1_500f).coerceIn(0f, 1f)
    val lengthStretch = ((letters - 5f) / 5f).coerceIn(0.25f, 1f)
    val attack = smoothstep((positionMs - token.startMs) / 260f)
    val release = smoothstep((token.endMs - positionMs) / 240f)
    return 1f + 0.58f * durationStretch * lengthStretch * min(attack, release)
}

/** Reuses the actual media timeline; the same lyric view follows playback and seeking. */
@Composable
internal fun rememberMotionLyricsPosition(song: Int, active: Boolean): State<Long> {
    val controls = LocalPlayerControls.current
    val timeline = LocalPlayerTimelineState.current
        ?: controls.mediaFor(song)?.let { controls.timelineFor(song) }
    return if (timeline != null) rememberLyricsVisualPosition(timeline, active)
    else remember { androidx.compose.runtime.mutableLongStateOf(0L) }
}

/** Kinetic, cinematic lyric typography over the shapes field. No plain/unsynced mode. */
@Composable
internal fun MotionLyrics(
    lyrics: Lyrics?,
    positionMs: State<Long>,
    modifier: Modifier = Modifier,
) {
    if (lyrics !is Lyrics.Word && lyrics !is Lyrics.Line) return
    val scheme = MaterialTheme.colorScheme
    // Use the higher-contrast pure black or white against the kinetic background.
    val lyricColor = if (scheme.primaryContainer.luminance() > 0.179f) Color.Black else Color.White

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val compact = maxWidth < 400.dp
        val fontSize = if (compact) 48.sp else 72.sp
        val style = MaterialTheme.typography.displaySmall.copy(
            fontSize = fontSize,
            lineHeight = if (compact) 54.sp else 80.sp,
            fontWeight = FontWeight.Black,
            shadow = Shadow(scheme.primaryContainer, Offset(1f, 3f), 12f),
        )
        if (lyrics is Lyrics.Word) {
            val words = remember(lyrics) { MotionWordIndex(groupMotionWords(lyrics.lines)) }
            // Only recompose when the displayed word changes. The letterform is
            // then animated entirely in a graphics layer, with no remeasurement.
            val currentWord = rememberLyricsStructure(words, positionMs) {
                words.at(positionMs.value)
            }
            val word = currentWord.value
            if (word != null) {
                val token = remember(word) { TimedToken(word.text, word.startMs, word.endMs) }
                val measurer = rememberTextMeasurer()
                val measuredWidth = remember(word, style, measurer) {
                    measurer.measure(word.text, style = style, softWrap = false, maxLines = 1).size.width
                }.coerceAtLeast(1)
                val maxScale = with(LocalDensity.current) {
                    maxWidth.toPx() / (measuredWidth + 2f * MotionLyricShadowInset.toPx())
                }
                Text(
                    text = word.text,
                    modifier = Modifier.wrapContentWidth(unbounded = true).graphicsLayer {
                        // Modulate alpha rather than creating an offscreen layer
                        // with a tight clip around the text and its shadow.
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                        val now = positionMs.value
                        val elapsed = (now - word.startMs).coerceAtLeast(0L)
                        val remaining = (word.endMs - now).coerceAtLeast(0L)
                        val duration = (word.endMs - word.startMs).coerceAtLeast(1L)
                        val travel = (duration / 250f).coerceIn(0.25f, 1f)
                        val attack = smoothstep(elapsed / motionTransitionMs(duration, 160f))
                        val release = smoothstep(remaining / motionTransitionMs(duration, 170f))
                        scaleX = min(
                            motionWordStretch(token, now) * (0.96f + 0.04f * attack),
                            maxScale,
                        )
                        translationX = ((1f - release) * 10.dp.toPx() -
                            (1f - attack) * 17.dp.toPx()) * travel
                        alpha = min(attack, release)
                        transformOrigin = TransformOrigin.Center
                    }.padding(MotionLyricShadowInset),
                    style = style,
                    color = lyricColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                )
            }
        } else if (lyrics is Lyrics.Line) {
            val timing = remember(lyrics) { LyricsTimingIndex.line(lyrics.lines) }
            // Compose only the lines within their transition windows. Their
            // actual opacity and motion are calculated from playback time in
            // graphicsLayer, so pausing freezes and seeking jumps to the
            // correct animation frame without restarting a tween.
            val visibleLines by rememberLyricsStructure(timing, positionMs) {
                val now = positionMs.value
                val candidate = timing.lineIndexAtOrBefore(now + LineLeadInMs)
                listOf(candidate - 1, candidate).filter { index ->
                    val line = timing.lines.getOrNull(index)?.line
                    line != null && now >= line.startMs - LineLeadInMs && now < line.endMs + LineTailMs
                }
            }
            val sentenceStyle = style.copy(
                fontSize = if (compact) 36.sp else 52.sp,
                lineHeight = if (compact) 41.sp else 57.sp,
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                visibleLines.forEach { index ->
                    val line = timing.lines[index].line as LineLyric
                    Text(
                        text = line.text,
                        modifier = Modifier.fillMaxWidth().graphicsLayer {
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                            val now = positionMs.value
                            // Former fade/slide transition timings are now
                            // anchored to the line's timestamps, not wall time.
                            val enterAt = line.startMs - LineLeadInMs
                            val leaveAt = line.endMs + LineTailMs
                            val visibleDuration = (leaveAt - enterAt).coerceAtLeast(1L)
                            val fadeIn = smoothstep((now - enterAt) /
                                motionTransitionMs(visibleDuration, 350f))
                            val fadeOut = smoothstep((leaveAt - now) /
                                motionTransitionMs(visibleDuration, 230f))
                            val slideIn = smoothstep((now - enterAt) /
                                motionTransitionMs(visibleDuration, 460f))
                            val slideOut = smoothstep((leaveAt - now) /
                                motionTransitionMs(visibleDuration, 360f))
                            val lineDuration = (line.endMs - line.startMs).coerceAtLeast(1L)
                            val elapsed = (now - line.startMs).coerceAtLeast(0L)
                            val entry = smoothstep(elapsed / motionTransitionMs(lineDuration, 480f))
                            val exit = smoothstep((line.endMs - now) /
                                motionTransitionMs(lineDuration, 330f))
                            alpha = min(fadeIn, fadeOut)
                            val horizontalTravel = min(size.width / 3f, 56.dp.toPx())
                            translationX = (1f - slideOut) * horizontalTravel * 0.75f -
                                (1f - slideIn) * horizontalTravel +
                                (1f - exit) * 9.dp.toPx() - (1f - entry) * 16.dp.toPx()
                            scaleX = 0.97f + 0.03f * entry
                        }.padding(MotionLyricShadowInset),
                        style = sentenceStyle,
                        textAlign = TextAlign.Center,
                        color = lyricColor,
                    )
                }
            }
        }
    }
}
