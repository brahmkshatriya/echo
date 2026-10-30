package dev.brahmkshatriya.echo.app.ui.player.song.lyrics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialShapes.Companion.Circle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.platform.VariableText
import dev.brahmkshatriya.echo.app.playback.PlayerTimelineState
import dev.brahmkshatriya.echo.app.ui.player.song.LyricsTransitionDurationMs
import dev.brahmkshatriya.echo.app.ui.theme.googleSansFontFamily
import kotlin.math.PI
import kotlin.math.cos

@Composable
fun NoLyricsTicker() {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Lyrics",
            style = typography.titleMedium,
            color = colorScheme.onPrimaryContainer,
            maxLines = 1,
        )
    }
}

private data class LyricsTickerContent(val lineIndex: Int?, val order: Int)

private fun lyricsTickerContent(
    timingIndex: LyricsTimingIndex,
    positionMs: Long,
): LyricsTickerContent {
    val previousIndex = timingIndex.lineIndexAtOrBefore(positionMs)

    // Never show the first lyric before its timestamp. Short pre-rolls are not
    // treated like inter-line gaps where showing the upcoming line is useful.
    if (previousIndex < 0) {
        val firstLine = timingIndex.lines.firstOrNull()?.line
        if (firstLine != null && positionMs < firstLine.startMs)
            return LyricsTickerContent(lineIndex = null, order = -1)
    }

    val previousLine = timingIndex.lines.getOrNull(previousIndex)?.line
    if (previousLine != null && positionMs <= previousLine.endMs)
        return LyricsTickerContent(lineIndex = previousIndex, order = previousIndex * 2)

    if (timingIndex.waitingGapAt(positionMs) != null)
        return LyricsTickerContent(lineIndex = null, order = previousIndex * 2 + 1)

    val nextIndex = previousIndex + 1
    val nextLine = timingIndex.lines.getOrNull(nextIndex)?.line
    return if (nextLine != null) LyricsTickerContent(lineIndex = nextIndex, order = nextIndex * 2)
    else LyricsTickerContent(lineIndex = null, order = previousIndex * 2 + 1)
}

@Composable
fun LyricsTicker(
    lyrics: Lyrics,
    timelineState: PlayerTimelineState,
    isPlaying: Boolean,
    active: Boolean = true,
) {
    val positionMs = rememberLyricsVisualPosition(timelineState, active)
    when (lyrics) {
        is Lyrics.Word -> TimedLyricsTicker(lyrics, timelineState, positionMs, active && isPlaying)
        is Lyrics.Line -> LineLyricsTicker(lyrics, positionMs, active && isPlaying)
        is Lyrics.Simple ->
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Lyrics",
                    style = typography.titleMedium,
                    color = colorScheme.onPrimaryContainer,
                    maxLines = 1,
                )
            }
    }
}

@Composable
private fun LineLyricsTicker(lyrics: Lyrics.Line, positionMs: State<Long>, isPlaying: Boolean) {
    val timingIndex = remember(lyrics.lines) { LyricsTimingIndex.line(lyrics.lines) }
    val waitingGapActive by
        rememberLyricsStructure(timingIndex, positionMs) {
            timingIndex.waitingGapAt(positionMs.value) != null
        }
    val content by
        rememberLyricsStructure(timingIndex, positionMs) {
            lyricsTickerContent(timingIndex, positionMs.value)
        }
    AnimatedContent(
        targetState = content,
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        transitionSpec = {
            val direction = if (targetState.order >= initialState.order) 1 else -1
            val animation =
                tween<IntOffset>(
                    durationMillis = LyricsTransitionDurationMs,
                    easing = FastOutSlowInEasing,
                )
            val enter =
                slideInVertically(animation) { direction * it } +
                    fadeIn(tween(LyricsTransitionDurationMs))
            val exit =
                slideOutVertically(animation) { -direction * it } +
                    fadeOut(tween(LyricsTransitionDurationMs))
            enter togetherWith exit
        },
    ) { target ->
        val lineTiming = target.lineIndex?.let(timingIndex.lines::getOrNull) as? LineTiming
        if (lineTiming == null)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LyricsWaitingDots(
                    animated = isPlaying && waitingGapActive,
                    modifier = Modifier.size(width = 32.dp, height = 32.dp),
                )
            }
        else LineLyricsMarquee(lineTiming = lineTiming, positionMs = { positionMs.value })
    }
}

@Composable
private fun LineLyricsMarquee(lineTiming: LineTiming, positionMs: () -> Long) {
    val line = lineTiming.line

    Layout(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        content = {
            Text(
                text = lineTiming.text,
                style = typography.titleMedium,
                color = colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
            )
        },
    ) { measurables, constraints ->
        val placeable =
            measurables
                .single()
                .measure(
                    Constraints(
                        minWidth = 0,
                        maxWidth = Constraints.Infinity,
                        minHeight = 0,
                        maxHeight = constraints.maxHeight,
                    )
                )
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val overflow = (placeable.width - width).coerceAtLeast(0)
        val y = (height - placeable.height) / 2
        layout(width, height) {
            val x =
                if (overflow == 0) (width - placeable.width) / 2
                else {
                    val durationMs = (line.endMs - line.startMs).coerceAtLeast(1L)
                    val elapsedMs = (positionMs() - line.startMs).coerceIn(0L, durationMs)

                    // Give both ends of an overflowing line some stationary reading time.
                    // The travel phase still covers the entire overflow before the line ends.
                    val startHoldMs = minOf(300L, durationMs / 5)
                    val endHoldMs = minOf(450L, durationMs / 4)
                    val travelMs = (durationMs - startHoldMs - endHoldMs).coerceAtLeast(1L)
                    val travelProgress =
                        when {
                            elapsedMs <= startHoldMs -> 0f
                            elapsedMs >= durationMs - endHoldMs -> 1f
                            else ->
                                ((elapsedMs - startHoldMs).toFloat() / travelMs.toFloat()).coerceIn(
                                    0f,
                                    1f,
                                )
                        }

                    -(overflow * travelProgress).toInt()
                }
            placeable.placeRelative(x, y)
        }
    }
}

@Composable
private fun TimedLyricsTicker(
    lyrics: Lyrics.Word,
    timelineState: PlayerTimelineState,
    positionMs: State<Long>,
    isPlaying: Boolean,
) {
    val timingIndex = remember(lyrics.lines) { LyricsTimingIndex.word(lyrics.lines) }
    val waitingGapActive by
        rememberLyricsStructure(timingIndex, positionMs) {
            timingIndex.waitingGapAt(positionMs.value) != null
        }
    val content by
        rememberLyricsStructure(timingIndex, positionMs) {
            lyricsTickerContent(timingIndex, positionMs.value)
        }
    AnimatedContent(
        targetState = content,
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        transitionSpec = {
            val direction = if (targetState.order >= initialState.order) 1 else -1
            val animation =
                tween<IntOffset>(
                    durationMillis = LyricsTransitionDurationMs,
                    easing = FastOutSlowInEasing,
                )
            val enter =
                slideInVertically(animation) { direction * it } +
                    fadeIn(tween(LyricsTransitionDurationMs))
            val exit =
                slideOutVertically(animation) { -direction * it } +
                    fadeOut(tween(LyricsTransitionDurationMs))
            enter togetherWith exit
        },
    ) { target ->
        val lineTiming = target.lineIndex?.let(timingIndex.lines::getOrNull) as? WordLineTiming
        if (lineTiming == null)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LyricsWaitingDots(
                    animated = isPlaying && waitingGapActive,
                    modifier = Modifier.size(width = 32.dp, height = 32.dp),
                )
            }
        else TimedLyricsLine(timingIndex, lineTiming, timelineState, positionMs)
    }
}

@Composable
fun LyricsWaitingDots(animated: Boolean, modifier: Modifier = Modifier) {
    val phase = remember { Animatable(0f) }
    val motionStrength by
        animateFloatAsState(
            targetValue = if (animated) 1f else 0f,
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            label = "Lyrics waiting dots motion strength",
        )
    LaunchedEffect(animated) {
        if (!animated) {
            phase.snapTo(0f)
            return@LaunchedEffect
        }
        while (true) {
            val start = phase.value % 1f
            phase.snapTo(start)
            phase.animateTo(
                targetValue = start + 1f,
                animationSpec = tween(durationMillis = 900, easing = LinearEasing),
            )
        }
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dotColor = colorScheme.onPrimaryContainer
        repeat(3) { index ->
            Box(
                Modifier.size(8.dp)
                    .graphicsLayer {
                        val pulse =
                            (cos(phase.value * 2f * PI.toFloat() - index * 2f * PI.toFloat() / 3f) +
                                1f) / 2f
                        val animatedScale = 0.55f + pulse * 0.45f
                        val animatedAlpha = 0.55f + pulse * 0.4f
                        val scale = 0.78f + (animatedScale - 0.78f) * motionStrength
                        val resolvedAlpha = 0.75f + (animatedAlpha - 0.75f) * motionStrength
                        scaleX = scale
                        scaleY = scaleX
                        alpha = resolvedAlpha
                    }
                    .clip(Circle.toShape())
                    .background(dotColor)
            )
        }
    }
}

@Composable
private fun TimedLyricsLine(
    timingIndex: LyricsTimingIndex,
    lineTiming: WordLineTiming,
    timelineState: PlayerTimelineState,
    positionMs: State<Long>,
) {
    val line = lineTiming.line
    if (line.tokens.isEmpty()) return

    val lineText = lineTiming.text
    if (lineText.isEmpty()) return

    val activeTokenIndex by
        rememberLyricsStructure(lineTiming, positionMs) {
            lineTiming.tokenIndexAt(positionMs.value)
        }
    val peakPosition =
        remember(timingIndex, timelineState, positionMs) {
            { timingIndex.peakPositionAt(positionMs.value, timelineState.durationMs.toLong()) }
        }
    val horizontalRevealPosition =
        remember(lineTiming, positionMs) { { lineTiming.revealPositionAt(positionMs.value) } }
    val bloomStrength = rememberLyricsBloomStrength()
    val bloom =
        remember(lineTiming, positionMs, bloomStrength) {
            {
                val strength = bloomStrength.value
                if (strength <= 0.001f) null
                else
                    lineTiming.bloomAt(positionMs.value)?.let {
                        it.copy(strength = it.strength * strength)
                    }
            }
        }

    val highlightedColor = colorScheme.onSurface
    val referenceFontFamily = googleSansFontFamily()
    val textMeasurer = rememberTextMeasurer()
    val titleMediumStyle = typography.titleMedium
    val referenceStyle =
        remember(referenceFontFamily, titleMediumStyle) {
            titleMediumStyle.copy(
                fontFamily = referenceFontFamily,
                fontWeight = FontWeight.Normal,
                fontFeatureSettings = "liga 0, kern 1",
            )
        }
    val marqueeLayout =
        remember(lineText, referenceStyle, textMeasurer) {
            textMeasurer.measure(
                text = lineText,
                style = referenceStyle,
                maxLines = 1,
                softWrap = false,
                constraints = Constraints(maxWidth = Constraints.Infinity),
            )
        }
    val glyphXPositions =
        remember(lineText, marqueeLayout) {
            FloatArray(lineText.length) { index ->
                marqueeLayout.getHorizontalPosition(index, usePrimaryDirection = true)
            }
        }
    val glyphRightPositions =
        remember(lineText, marqueeLayout) {
            FloatArray(lineText.length) { index -> marqueeLayout.getBoundingBox(index).right }
        }
    val glyphBaselines =
        remember(lineText, marqueeLayout) {
            val baseline = marqueeLayout.getLineBaseline(0)
            FloatArray(lineText.length) { baseline }
        }
    val focusTokenIndex = activeTokenIndex.coerceAtLeast(0).coerceAtMost(line.tokens.lastIndex)
    val targetFocusX = marqueeLayout.tokenCenter(lineTiming, focusTokenIndex)
    val focusX = animateFloatAsState(targetValue = targetFocusX, animationSpec = tween())

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val viewportWidth =
            if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat()
            else marqueeLayout.size.width.toFloat()
        val horizontalInsetPx = with(density) { 8.dp.toPx() }
        val marqueeViewportWidth = (viewportWidth - horizontalInsetPx * 2f).coerceAtLeast(0f)
        val viewportHeight =
            if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat()
            else marqueeLayout.size.height.toFloat()
        val offsetX =
            if (marqueeLayout.size.width <= marqueeViewportWidth)
                (viewportWidth - marqueeLayout.size.width) / 2f
            else viewportWidth / 2f
        val offsetY = (viewportHeight - marqueeLayout.size.height) / 2f

        VariableText(
            modifier =
                Modifier.fillMaxSize().drawWithContent {
                    val translation =
                        if (marqueeLayout.size.width > marqueeViewportWidth) -focusX.value else 0f
                    translate(left = translation) { this@drawWithContent.drawContent() }
                },
            text = lineText,
            peakPosition = peakPosition,
            glyphXPositions = glyphXPositions,
            glyphBaselines = glyphBaselines,
            glyphPeakPositions = lineTiming.globalGlyphPositions,
            glyphRightPositions = glyphRightPositions,
            horizontalRevealPosition = horizontalRevealPosition,
            bloom = bloom,
            offsetX = offsetX,
            offsetY = offsetY,
            color = highlightedColor,
            fontSize = typography.titleMedium.fontSize,
        )
    }
}

private fun TextLayoutResult.tokenCenter(lineTiming: WordLineTiming, tokenIndex: Int): Float {
    val token = lineTiming.line.tokens[tokenIndex]
    val startOffset = lineTiming.tokenOffsets[tokenIndex].coerceAtMost(layoutInput.text.lastIndex)
    val endOffset =
        (startOffset + token.text.length - 1).coerceIn(startOffset, layoutInput.text.lastIndex)
    return (getBoundingBox(startOffset).left + getBoundingBox(endOffset).right) / 2f
}
