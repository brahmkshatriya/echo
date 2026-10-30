package dev.brahmkshatriya.echo.app.ui.player.song.lyrics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.platform.LyricsMinAlpha
import dev.brahmkshatriya.echo.app.platform.VariableText
import dev.brahmkshatriya.echo.app.playback.PlayerTimelineState

// Timed lyrics and the kinetic-shapes view share one viewport fade geometry.
val FullLyricsTopFadeHeight = 130.dp
val FullLyricsBottomFadeHeight = 160.dp

private enum class LyricsLinePeakRegion {
    Before,
    Within,
    After,
}

@Composable
fun FullLyricsGapIndicator(visible: Boolean, animated: Boolean, modifier: Modifier = Modifier) {
    val alpha = remember { Animatable(0f) }
    val expansion = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            expansion.animateTo(1f, tween(200))
            alpha.animateTo(1f, tween(200))
        } else {
            alpha.animateTo(0f, tween(200))
            expansion.animateTo(0f, tween(200))
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(FullLyricsGapIndicatorHeight)
                .padding(top = 16.dp)
                .graphicsLayer {
                    this.alpha = alpha.value
                    scaleY = expansion.value
                    clip = true
                },
        contentAlignment = Alignment.Center,
    ) {
        LyricsWaitingDots(
            animated = animated && visible,
            modifier = Modifier.size(width = 32.dp, height = 32.dp),
        )
    }
}

@Composable
fun FullLyricsTimelineGapIndicator(
    waitingGap: LyricsWaitingGap,
    positionMs: State<Long>,
    isPlaying: Boolean,
    autoScrollSuppressed: Boolean,
    modifier: Modifier = Modifier,
) {
    val visible by
        remember(waitingGap, positionMs, autoScrollSuppressed) {
            derivedStateOf {
                !autoScrollSuppressed && waitingGap.isVisibleInFullLyrics(positionMs.value)
            }
        }
    val animated by
        remember(waitingGap, positionMs, isPlaying) {
            derivedStateOf { isPlaying && waitingGap.contains(positionMs.value) }
        }
    FullLyricsGapIndicator(visible = visible, animated = animated, modifier = modifier)
}

fun Modifier.lyricsEdgeFade(
    topFadeHeight: Dp = 64.dp,
    bottomFadeHeight: Dp = topFadeHeight,
    viewportHeightPx: (() -> Float)? = null,
    backgroundColor: Color = Color.Unspecified,
): Modifier {
    val masked = backgroundColor == Color.Unspecified
    val layer =
        if (masked) graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen } else this
    return layer.drawWithCache {
        val top = topFadeHeight.toPx().coerceIn(0f, size.height)
        val bottom = bottomFadeHeight.toPx().coerceIn(0f, size.height)
        val blendMode = if (masked) BlendMode.DstIn else BlendMode.SrcOver
        val edgeStart = if (masked) Color.Transparent else backgroundColor
        val edgeEnd = if (masked) Color.Black else backgroundColor.copy(alpha = 0f)
        val topMask =
            if (top > 0f)
                Brush.verticalGradient(colors = listOf(edgeStart, edgeEnd), startY = 0f, endY = top)
            else null
        val bottomMask =
            if (bottom > 0f)
                Brush.verticalGradient(
                    colors = listOf(edgeEnd, edgeStart),
                    startY = 0f,
                    endY = bottom,
                )
            else null

        onDrawWithContent {
            drawContent()

            if (top > 0f)
                drawRect(brush = topMask!!, size = Size(size.width, top), blendMode = blendMode)

            if (bottom > 0f) {
                val height = (viewportHeightPx?.invoke() ?: size.height).coerceIn(0f, size.height)
                translate(top = height - bottom) {
                    drawRect(
                        brush = bottomMask!!,
                        size = Size(size.width, bottom),
                        blendMode = blendMode,
                    )
                }
            }
        }
    }
}

/** Cull buffered rows without creating an offscreen layer for each edge row. */
fun Modifier.lyricsItemViewport(itemTopPx: () -> Float, viewportHeightPx: () -> Float): Modifier =
    drawWithCache {
        onDrawWithContent {
            val itemTop = itemTopPx()
            val viewportHeight = viewportHeightPx().coerceAtLeast(0f)
            if (itemTop + size.height > 0f && itemTop < viewportHeight) drawContent()
        }
    }

@Composable
fun FullTimedLyricsLine(
    timingIndex: LyricsTimingIndex,
    lineTiming: WordLineTiming,
    lineLayout: FullLyricsLineLayout,
    timelineState: PlayerTimelineState,
    positionMs: State<Long>,
    isActive: Boolean,
    isPast: Boolean,
    onClick: () -> Unit,
) {
    val line = lineTiming.line
    val bloomStrength = rememberLyricsBloomStrength()
    val lineEmphasis =
        animateFloatAsState(
            targetValue = if (isActive || isPast) 1f else 0f,
            animationSpec = tween(360, easing = FastOutSlowInEasing),
        )
    val lyricColor = colorScheme.onSurface
    val interactionSource = remember(line) { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val interactionAlpha by
        animateFloatAsState(
            targetValue =
                when {
                    isPressed -> 0.12f
                    isHovered -> 0.08f
                    else -> 0f
                },
            animationSpec = tween(120),
        )
    val lineText = lineTiming.text
    val textAlign = line.position.textAlign

    val rowHeight = with(LocalDensity.current) { lineLayout.heightPx.toDp() }
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .height(rowHeight)
                .background(lyricColor.copy(alpha = interactionAlpha), RoundedCornerShape(8.dp))
                .graphicsLayer { alpha = 0.32f + 0.68f * lineEmphasis.value }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(FullLyricsLinePadding),
        verticalArrangement = Arrangement.spacedBy(FullLyricsAuxiliaryGap),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val density = LocalDensity.current
            val referenceLayout = lineLayout.textLayout
            val referenceStyle = referenceLayout.layoutInput.style
            val textHeight = with(density) { referenceLayout.size.height.toDp() }

            if (lineText.isNotEmpty()) {
                // The peak spans line boundaries. Upcoming and completed rows
                // must use the same variable font to show both sides of its trail.
                val peakBounds =
                    remember(lineTiming) {
                        val radius = lineText.lastIndex / 4f
                        val positions = lineTiming.globalGlyphPositions
                        (positions.minOrNull()!! - radius)..(positions.maxOrNull()!! + radius)
                    }
                val peakRegion by
                    rememberLyricsStructure(timingIndex, timelineState, positionMs, peakBounds) {
                        val peak =
                            timingIndex.peakPositionAt(
                                positionMs.value,
                                timelineState.durationMs.toLong(),
                            ) ?: 0f
                        when {
                            peak < peakBounds.start -> LyricsLinePeakRegion.Before
                            peak > peakBounds.endInclusive -> LyricsLinePeakRegion.After
                            else -> LyricsLinePeakRegion.Within
                        }
                    }
                val peakPosition =
                    remember(timingIndex, timelineState, positionMs, peakBounds, peakRegion) {
                        when (peakRegion) {
                            // Fixed positions beyond the wave preserve the same
                            // weight/roundness without redrawing distant lines each frame.
                            LyricsLinePeakRegion.Before -> {
                                { peakBounds.start - 1f }
                            }
                            LyricsLinePeakRegion.After -> {
                                { peakBounds.endInclusive + 1f }
                            }
                            LyricsLinePeakRegion.Within -> {
                                {
                                    timingIndex.peakPositionAt(
                                        positionMs.value,
                                        timelineState.durationMs.toLong(),
                                    )
                                }
                            }
                        }
                    }
                val revealPosition =
                    remember(lineTiming, positionMs, isPast, lineText) {
                        derivedStateOf {
                            // Before the first word this is already zero, so the
                            // reveal never jumps backward when the row activates.
                            if (isPast) lineText.length.toFloat()
                            else lineTiming.revealPositionAt(positionMs.value)
                        }
                    }
                val horizontalRevealPosition = remember(revealPosition) { { revealPosition.value } }
                val bloom =
                    remember(lineTiming, positionMs, isActive, isPast, bloomStrength) {
                        if (isActive && !isPast) {
                            {
                                val strength = bloomStrength.value
                                if (strength <= 0.001f) null
                                else
                                    lineTiming.bloomAt(positionMs.value)?.let {
                                        it.copy(strength = it.strength * strength)
                                    }
                            }
                        } else null
                    }
                val glyphXPositions =
                    remember(lineText, referenceLayout) {
                        FloatArray(lineText.length) { index ->
                            referenceLayout.getHorizontalPosition(index, usePrimaryDirection = true)
                        }
                    }
                val glyphRightPositions =
                    remember(lineText, referenceLayout) {
                        FloatArray(lineText.length) { index ->
                            referenceLayout.getBoundingBox(index).right
                        }
                    }
                val glyphBaselines =
                    remember(lineText, referenceLayout) {
                        FloatArray(lineText.length) { index ->
                            referenceLayout.getLineBaseline(referenceLayout.getLineForOffset(index))
                        }
                    }

                VariableText(
                    modifier = Modifier.fillMaxWidth().height(textHeight),
                    text = lineText,
                    peakPosition = peakPosition,
                    glyphXPositions = glyphXPositions,
                    glyphBaselines = glyphBaselines,
                    glyphPeakPositions = lineTiming.globalGlyphPositions,
                    glyphRightPositions = glyphRightPositions,
                    // Use the same progress for both fades: the upcoming base
                    // stays at 32%, then brightens continuously as words reveal.
                    horizontalRevealPosition = horizontalRevealPosition,
                    bloom = bloom,
                    unrevealedAlpha = { 1f - (1f - LyricsMinAlpha) * lineEmphasis.value },
                    color = lyricColor,
                    fontSize = typography.headlineMedium.fontSize,
                )
            } else
                Text(
                    text = lineText,
                    modifier = Modifier.fillMaxWidth().height(textHeight),
                    style = referenceStyle,
                )
        }

        line.backgroundVocals
            .takeIf { it.isNotEmpty() }
            ?.let { vocals ->
                Text(
                    text = vocals.joinToString("") { it.text + it.trailingSpace },
                    modifier = Modifier.fillMaxWidth(),
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    textAlign = textAlign,
                    color = lyricColor.copy(alpha = 0.72f),
                )
            }
        line.translations.forEach { translation ->
            Text(
                text = translation.text,
                modifier = Modifier.fillMaxWidth(),
                style = typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
                color = lyricColor.copy(alpha = 0.78f),
            )
        }
    }
}

@Composable
fun FullLineLyricsLine(
    lineTiming: LineTiming,
    lineLayout: FullLyricsLineLayout,
    isActive: Boolean,
    isPast: Boolean,
    onClick: () -> Unit,
) {
    val line = lineTiming.line
    val textAlign = line.position.textAlign
    val lineAlpha by
        animateFloatAsState(
            targetValue = if (isActive || isPast) 1f else 0.32f,
            animationSpec = tween(360, easing = FastOutSlowInEasing),
        )
    val lyricColor = colorScheme.onSurface
    val interactionSource = remember(line) { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val interactionAlpha by
        animateFloatAsState(
            targetValue =
                when {
                    isPressed -> 0.12f
                    isHovered -> 0.08f
                    else -> 0f
                },
            animationSpec = tween(120),
        )

    val rowHeight = with(LocalDensity.current) { lineLayout.heightPx.toDp() }
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .height(rowHeight)
                .background(lyricColor.copy(alpha = interactionAlpha), RoundedCornerShape(8.dp))
                .graphicsLayer { alpha = lineAlpha }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(FullLyricsLinePadding),
        verticalArrangement = Arrangement.spacedBy(FullLyricsAuxiliaryGap),
    ) {
        Text(
            text = lineTiming.text,
            modifier = Modifier.fillMaxWidth(),
            style = typography.headlineMedium,
            lineHeight = typography.headlineLarge.lineHeight,
            fontWeight = FontWeight.Normal,
            textAlign = textAlign,
        )
        line.translations.forEach { translation ->
            Text(
                text = translation.text,
                modifier = Modifier.fillMaxWidth(),
                style = typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
                color = lyricColor.copy(alpha = 0.78f),
            )
        }
    }
}
