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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.platform.VariableText
import dev.brahmkshatriya.echo.app.ui.player.PlayerTimelineState
import dev.brahmkshatriya.echo.app.ui.theme.googleSansFontFamily

@Composable
internal fun FullLyricsGapIndicator(
    visible: Boolean,
    animated: Boolean,
    modifier: Modifier = Modifier,
) {
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
        modifier = modifier
            .fillMaxWidth()
            .height(FullLyricsGapIndicatorHeight * expansion.value)
            .padding(top = 16.dp * expansion.value)
            .graphicsLayer {
                this.alpha = alpha.value
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
internal fun FullLyricsTimelineGapIndicator(
    waitingGap: LyricsWaitingGap,
    timelineState: PlayerTimelineState,
    isPlaying: Boolean,
    autoScrollSuppressed: Boolean,
    modifier: Modifier = Modifier,
) {
    val visible by remember(waitingGap, timelineState, autoScrollSuppressed) {
        derivedStateOf {
            !autoScrollSuppressed &&
                    waitingGap.isVisibleInFullLyrics(timelineState.positionMs.toLong())
        }
    }
    val animated by remember(waitingGap, timelineState, isPlaying) {
        derivedStateOf {
            isPlaying && waitingGap.contains(timelineState.positionMs.toLong())
        }
    }
    FullLyricsGapIndicator(
        visible = visible,
        animated = animated,
        modifier = modifier,
    )
}

internal fun Modifier.lyricsEdgeFade(
    topFadeHeight: Dp = 64.dp,
    bottomFadeHeight: Dp = topFadeHeight,
): Modifier =
    graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }.drawWithCache {
        val topFraction = if (size.height > 0f) (topFadeHeight.toPx() / size.height).coerceIn(0f, 0.5f) else 0f
        val bottomFraction = if (size.height > 0f) (bottomFadeHeight.toPx() / size.height).coerceIn(0f, 0.5f) else 0f
        val mask = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                topFraction to Color.Black,
                (1f - bottomFraction) to Color.Black,
                1f to Color.Transparent
            )
        )

        onDrawWithContent {
            drawContent()
            drawRect(
                brush = mask,
                blendMode = BlendMode.DstIn
            )
        }
    }

@Composable
internal fun FullTimedLyricsLine(
    timingIndex: LyricsTimingIndex,
    lineTiming: WordLineTiming,
    timelineState: PlayerTimelineState,
    isActive: Boolean,
    isPast: Boolean,
    isLatestCompleted: Boolean,
    onClick: () -> Unit,
) {
    val line = lineTiming.line
    val lineAlpha by animateFloatAsState(
        targetValue = if (isActive || isPast) 1f else 0.32f,
        animationSpec = tween(360, easing = FastOutSlowInEasing)
    )
    val lyricColor = colorScheme.onPrimaryContainer
    val interactionSource = remember(line) { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val interactionAlpha by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.12f
            isHovered -> 0.08f
            else -> 0f
        },
        animationSpec = tween(120)
    )
    val lineText = lineTiming.text
    val textAlign = line.position.textAlign

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                lyricColor.copy(alpha = interactionAlpha),
                RoundedCornerShape(8.dp)
            ).graphicsLayer {
                alpha = lineAlpha
            }.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val density = LocalDensity.current
            val maxWidthPx = with(density) { maxWidth.roundToPx() }
            val referenceFontFamily = googleSansFontFamily()
            val textMeasurer = rememberTextMeasurer()
            val referenceStyle = typography.headlineMedium.copy(
                fontFamily = referenceFontFamily,
                fontWeight = FontWeight.Normal,
                fontFeatureSettings = "liga 0, kern 1",
                lineHeight = typography.headlineLarge.lineHeight,
                textAlign = textAlign,
            )
            val referenceLayout = remember(
                lineText,
                maxWidthPx,
                referenceStyle,
                textMeasurer,
            ) {
                textMeasurer.measure(
                    text = lineText,
                    style = referenceStyle,
                    softWrap = true,
                    maxLines = Int.MAX_VALUE,
                    constraints = Constraints(
                        minWidth = maxWidthPx,
                        maxWidth = maxWidthPx,
                    ),
                )
            }
            val textHeight = with(density) { referenceLayout.size.height.toDp() }

            if ((isActive || isPast) && lineText.isNotEmpty()) {
                val peakPosition = remember(
                    timingIndex,
                    timelineState,
                    isPast,
                    isLatestCompleted,
                ) {
                    if (isPast && !isLatestCompleted) {
                        { null }
                    } else {
                        {
                            timingIndex.peakPositionAt(
                                timelineState.positionMs.toLong(),
                                timelineState.durationMs.toLong(),
                            )
                        }
                    }
                }
                val horizontalRevealPosition = remember(
                    lineTiming,
                    timelineState,
                    isActive,
                    isPast,
                    isLatestCompleted,
                    lineText,
                ) {
                    when {
                        isActive && !isPast -> {
                            { lineTiming.revealPositionAt(timelineState.positionMs.toLong()) }
                        }
                        isPast && isLatestCompleted -> {
                            { lineText.length.toFloat() }
                        }
                        else -> null
                    }
                }
                val bloom = remember(lineTiming, timelineState, isActive, isPast) {
                    if (isActive && !isPast) {
                        { lineTiming.bloomAt(timelineState.positionMs.toLong()) }
                    } else {
                        null
                    }
                }
                val glyphXPositions = remember(lineText, referenceLayout) {
                    FloatArray(lineText.length) { index ->
                        referenceLayout.getHorizontalPosition(
                            index,
                            usePrimaryDirection = true,
                        )
                    }
                }
                val glyphRightPositions = remember(lineText, referenceLayout) {
                    FloatArray(lineText.length) { index ->
                        referenceLayout.getBoundingBox(index).right
                    }
                }
                val glyphBaselines = remember(lineText, referenceLayout) {
                    FloatArray(lineText.length) { index ->
                        referenceLayout.getLineBaseline(
                            referenceLayout.getLineForOffset(index)
                        )
                    }
                }

                VariableText(
                    text = lineText,
                    peakPosition = peakPosition,
                    glyphXPositions = glyphXPositions,
                    glyphBaselines = glyphBaselines,
                    glyphPeakPositions = lineTiming.globalGlyphPositions,
                    glyphRightPositions = glyphRightPositions,
                    horizontalRevealPosition = horizontalRevealPosition,
                    bloom = bloom,
                    color = lyricColor,
                    fontSize = typography.headlineMedium.fontSize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(textHeight),
                )
            } else {
                Text(
                    text = lineText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(textHeight),
                    style = referenceStyle,
                )
            }
        }

        line.backgroundVocals.takeIf { it.isNotEmpty() }?.let { vocals ->
            Text(
                text = vocals.joinToString("") { it.text + it.trailingSpace },
                modifier = Modifier.fillMaxWidth(),
                style = typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
                color = lyricColor.copy(alpha = 0.72f)
            )
        }
        line.translations.forEach { translation ->
            Text(
                text = translation.text,
                modifier = Modifier.fillMaxWidth(),
                style = typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
                color = lyricColor.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
internal fun FullLineLyricsLine(
    lineTiming: LineTiming,
    isActive: Boolean,
    isPast: Boolean,
    onClick: () -> Unit,
) {
    val line = lineTiming.line
    val textAlign = line.position.textAlign
    val lineAlpha by animateFloatAsState(
        targetValue = if (isActive || isPast) 1f else 0.32f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
    )
    val lyricColor = colorScheme.onPrimaryContainer
    val interactionSource = remember(line) { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val interactionAlpha by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.12f
            isHovered -> 0.08f
            else -> 0f
        },
        animationSpec = tween(120),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                lyricColor.copy(alpha = interactionAlpha),
                RoundedCornerShape(8.dp),
            )
            .graphicsLayer { alpha = lineAlpha }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
