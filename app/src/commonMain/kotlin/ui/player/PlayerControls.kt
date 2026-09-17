package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.onPointerScrollY
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.platform.hasTouchInput
import dev.brahmkshatriya.echo.app.ui.components.SquigglySlider
import echo.app.generated.resources.Res
import echo.app.generated.resources.ic_favorite
import echo.app.generated.resources.ic_favorite_filled
import echo.app.generated.resources.ic_more_vert
import echo.app.generated.resources.ic_pause
import echo.app.generated.resources.ic_pause_32
import echo.app.generated.resources.ic_play_arrow
import echo.app.generated.resources.ic_play_arrow_32
import echo.app.generated.resources.ic_repeat
import echo.app.generated.resources.ic_shuffle
import echo.app.generated.resources.ic_skip_next
import echo.app.generated.resources.ic_skip_next_32
import echo.app.generated.resources.ic_skip_previous
import echo.app.generated.resources.ic_skip_previous_32
import echo.app.generated.resources.ic_volume_off
import echo.app.generated.resources.ic_volume_up
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

@Composable
internal fun RepeatButton(modifier: Modifier = Modifier) {
    val controls = LocalPlayerControls.current
    val enabled = controls?.repeatEnabled == true
    IconButton(
        onClick = { controls?.repeatEnabled = !enabled },
        enabled = controls != null,
        modifier = modifier,
        shapes = IconButtonDefaults.shapes(),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (enabled) colorScheme.primary else Color.Transparent,
            contentColor = if (enabled) colorScheme.onPrimary else colorScheme.onPrimaryContainer
        )
    ) {
        Icon(
            painterResource(Res.drawable.ic_repeat),
            contentDescription = if (enabled) "Disable repeat" else "Enable repeat"
        )
    }
}

@Composable
internal fun PreviousButton(modifier: Modifier = Modifier, large: Boolean = false) {
    val timelineState = LocalPlayerTimelineState.current
    val pagerState = LocalPlayerPagerState.current
    val scope = rememberCoroutineScope()
    IconButton(
        onClick = {
            val timeline = timelineState ?: return@IconButton
            if (timeline.positionMs > 3_000f) {
                timeline.positionMs = 0f
            } else {
                timeline.positionMs = 0f
                scope.launch { pagerState?.playPrevious() }
            }
        },
        enabled = timelineState != null,
        modifier = modifier,
        shapes = IconButtonDefaults.shapes()
    ) {
        Icon(
            painterResource(
                if (large) Res.drawable.ic_skip_previous_32
                else Res.drawable.ic_skip_previous
            ),
            contentDescription = "Previous song"
        )
    }
}

@Composable
internal fun PlayPauseButton(modifier: Modifier = Modifier, large: Boolean = false) {
    val controls = LocalPlayerControls.current
    val isPlaying = controls?.isPlaying == true
    FilledIconButton(
        onClick = { controls?.isPlaying = !isPlaying },
        enabled = controls != null,
        modifier = modifier,
    ) {
        Icon(
            painterResource(
                when {
                    isPlaying && large -> Res.drawable.ic_pause_32
                    isPlaying -> Res.drawable.ic_pause
                    large -> Res.drawable.ic_play_arrow_32
                    else -> Res.drawable.ic_play_arrow
                }
            ),
            contentDescription = if (isPlaying) "Pause" else "Play"
        )
    }
}

@Composable
internal fun CompactPlayPauseButton(modifier: Modifier = Modifier) {
    val timelineState = LocalPlayerTimelineState.current
    val progress = if (timelineState == null || timelineState.durationMs <= 0f) {
        0f
    } else {
        (timelineState.positionMs / timelineState.durationMs).coerceIn(0f, 1f)
    }
    val progressColor = colorScheme.primary
    val backgroundColor = colorScheme.primary.copy(alpha = 0.33f)

    Box(
        modifier = modifier.requiredSize(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val outerRadius = 23.dp.toPx()
            drawCircle(
                color = backgroundColor,
                radius = outerRadius,
            )

            if (progress > 0f) {
                val strokeWidth = 1.5.dp.toPx()
                val progressRadius = 21.5.dp.toPx()
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - progressRadius,
                        center.y - progressRadius,
                    ),
                    size = Size(
                        progressRadius * 2f,
                        progressRadius * 2f,
                    ),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }

        PlayPauseButton(Modifier.fillMaxSize())
    }
}

@Composable
internal fun NextButton(modifier: Modifier = Modifier, large: Boolean = false) {
    val timelineState = LocalPlayerTimelineState.current
    val controls = LocalPlayerControls.current
    val pagerState = LocalPlayerPagerState.current
    val scope = rememberCoroutineScope()
    IconButton(
        onClick = {
            timelineState?.positionMs = 0f
            scope.launch { pagerState?.playNext(controls?.shuffleEnabled == true) }
        },
        enabled = timelineState != null,
        modifier = modifier,
        shapes = IconButtonDefaults.shapes()
    ) {
        Icon(
            painterResource(
                if (large) Res.drawable.ic_skip_next_32 else Res.drawable.ic_skip_next
            ),
            contentDescription = "Next song"
        )
    }
}

@Composable
internal fun ShuffleButton(modifier: Modifier = Modifier) {
    val controls = LocalPlayerControls.current
    val enabled = controls?.shuffleEnabled == true
    IconButton(
        onClick = { controls?.shuffleEnabled = !enabled },
        enabled = controls != null,
        modifier = modifier,
        shapes = IconButtonDefaults.shapes(),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (enabled) colorScheme.primary else Color.Transparent,
            contentColor = if (enabled) colorScheme.onPrimary else colorScheme.onPrimaryContainer
        )
    ) {
        Icon(
            painterResource(Res.drawable.ic_shuffle),
            contentDescription = if (enabled) "Disable shuffle" else "Enable shuffle"
        )
    }
}

@Composable
fun Controller() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(8.dp).padding(bottom = 8.dp)
    ) {
        ShuffleButton(Modifier.size(48.dp))
        PreviousButton(Modifier.size(56.dp), large = true)
        PlayPauseButton(Modifier.size(56.dp), large = true)
        NextButton(Modifier.size(56.dp), large = true)
        RepeatButton(Modifier.size(48.dp))
    }
}

@Composable
fun ExpandedTimeline(
    i: Int,
    lyricsVisible: Boolean = false,
    rowsCollapseProgress: Float = 0f,
    onArtworkClick: () -> Unit = {},
) {
    val timelineState = LocalPlayerTimelineState.current ?: return
    val maxRange = timelineState.durationMs
    var showRemainingTime by remember { mutableStateOf(false) }
    val currentTimeLabel by remember {
        derivedStateOf { formatTime(timelineState.positionMs) }
    }
    val endLabel by remember(showRemainingTime) {
        derivedStateOf {
            if (showRemainingTime) {
                "-${formatTime(maxRange - timelineState.positionMs)}"
            } else {
                formatTime(maxRange)
            }
        }
    }
    val mergedStyle = LocalTextStyle.current.merge(typography.labelLarge)
    val collapseProgress = rowsCollapseProgress.coerceIn(0f, 1f)
    val secondaryRowsAlpha = 1f - collapseProgress
    val compactPlayPauseVisible = lyricsVisible && collapseProgress >= 0.5f

    SubcomposeLayout(Modifier.fillMaxWidth().clipToBounds()) { constraints ->
        val looseConstraints = constraints.copy(minHeight = 0)
        val metadata = subcompose("metadata") {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(lyricsVisible) {
                    PlayerArtwork(
                        contentDescription = "Song $i artwork",
                        modifier = Modifier
                            .padding(start = 16.dp, end = 8.dp)
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorScheme.primaryFixed)
                            .clickable(onClick = onArtworkClick),
                    )
                }
                AnimatedVisibility(!lyricsVisible) {
                    Spacer(Modifier.width(16.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Song $i")
                    Text("Artist $i", color = colorScheme.primary)
                }
                Spacer(Modifier.width(8.dp))
                LikeToggle()
                AnimatedVisibility(compactPlayPauseVisible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(8.dp))
                        CompactPlayPauseButton()
                    }
                }
                Spacer(Modifier.width(8.dp))
                MoreButton()
                Spacer(Modifier.width(8.dp))
            }
        }.single().measure(looseConstraints)

        val times = subcompose("times") {
            Row(
                Modifier.fillMaxWidth()
                    .graphicsLayer { alpha = secondaryRowsAlpha }
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = currentTimeLabel,
                    style = mergedStyle,
                    modifier = Modifier.padding(8.dp),
                )
                Spacer(Modifier.weight(1f))
                val endInteraction = remember { MutableInteractionSource() }
                Text(
                    text = endLabel,
                    style = mergedStyle,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(interactionSource = endInteraction) {
                            showRemainingTime = !showRemainingTime
                        }
                        .padding(8.dp),
                )
            }
        }.single().measure(looseConstraints)

        val sliderHorizontalPadding = 16.dp.roundToPx()
        val sliderWidth = (constraints.maxWidth - sliderHorizontalPadding * 2).coerceAtLeast(0)
        val slider = subcompose("slider") {
            PlayerSlider(
                modifier = Modifier.fillMaxWidth()
                    .height(72.dp)
                    .graphicsLayer { alpha = secondaryRowsAlpha },
                timelineState = timelineState,
            )
        }.single().measure(
            Constraints(
                minWidth = sliderWidth,
                maxWidth = sliderWidth,
                minHeight = 0,
                maxHeight = looseConstraints.maxHeight,
            )
        )

        val formatBadge = subcompose("format") {
            Text(
                text = "FLAC",
                style = mergedStyle,
                modifier = Modifier
                    .graphicsLayer { alpha = secondaryRowsAlpha }
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(100))
                    .background(colorScheme.primary.copy(0.1f))
                    .clickable {}
                    .padding(12.dp, 4.dp),
            )
        }.single().measure(looseConstraints.copy(minWidth = 0))

        val metadataToTimesSpacing = 40.dp.roundToPx()
        val sliderTop = (metadata.height - 12.dp.roundToPx()).coerceAtLeast(0)
        val expandedContentHeight = maxOf(
            metadata.height + metadataToTimesSpacing + times.height,
            sliderTop + slider.height,
            formatBadge.height,
        ).coerceIn(constraints.minHeight, constraints.maxHeight)
        val collapsedContentHeight = metadata.height.coerceIn(
            constraints.minHeight,
            constraints.maxHeight,
        )
        val contentHeight = (
            expandedContentHeight +
                    (collapsedContentHeight - expandedContentHeight) * collapseProgress
            ).roundToInt().coerceIn(constraints.minHeight, constraints.maxHeight)
        val secondaryRowsOffset = (
            (expandedContentHeight - collapsedContentHeight) * collapseProgress
            ).roundToInt()

        layout(constraints.maxWidth, contentHeight) {
            slider.placeRelative(sliderHorizontalPadding, sliderTop + secondaryRowsOffset)
            metadata.placeRelative(0, 0)
            times.placeRelative(0, metadata.height + metadataToTimesSpacing + secondaryRowsOffset)
            formatBadge.placeRelative(
                x = (constraints.maxWidth - formatBadge.width) / 2,
                y = expandedContentHeight - formatBadge.height + secondaryRowsOffset,
            )
        }
    }
}

@Composable
fun MoreButton() {
    IconButton(
        onClick = { },
        modifier = Modifier.height(48.dp).width(32.dp),
        shapes = IconButtonDefaults.shapes()
    ) {
        Icon(
            painterResource(Res.drawable.ic_more_vert),
            contentDescription = "Close Player"
        )
    }
}

@Composable
fun LikeToggle() {
    val favourite = remember { mutableStateOf(true) }
    val interactionSource = remember { MutableInteractionSource() }
    FilledTonalIconToggleButton(
        checked = favourite.value,
        onCheckedChange = { favourite.value = it },
        modifier = Modifier.size(44.dp),
        shapes = IconButtonDefaults.toggleableShapes(
            checkedShape = RoundedCornerShape(100)
        ),
        interactionSource = interactionSource,
        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
            colorScheme.primary.copy(0.25f),
            colorScheme.onPrimaryContainer,
            checkedContentColor = colorScheme.tertiaryContainer,
            checkedContainerColor = colorScheme.onTertiaryContainer
        ),
    ) {
        Icon(
            painterResource(
                if (favourite.value) Res.drawable.ic_favorite_filled
                else Res.drawable.ic_favorite
            ),
            contentDescription = if (favourite.value) "Favourite" else "Unfavourite"
        )
    }
}


@Composable
fun Modifier.playerBackground(colored: Boolean = false): Modifier {
    val playerSheet = LocalPlayerSheet.current
    val playerPadding = LocalPlayerPadding.current
    val peekHeight = playerSheet?.peekHeight ?: 80.dp
    val layoutDirection = LocalLayoutDirection.current
    val startPadding = playerPadding.calculateStartPadding(layoutDirection)
    val endPadding = playerPadding.calculateEndPadding(layoutDirection)
    val animatedStart = animateDpAsState(
        startPadding + collapsedHorizontalPadding.dp, tween()
    )
    val animatedEnd = animateDpAsState(
        endPadding + collapsedHorizontalPadding.dp, tween()
    )

    return fillMaxSize().graphicsLayer {
        val sheetProgress = playerSheet?.progressState?.floatValue ?: 0f
        val positiveProgress = sheetProgress.coerceIn(0f, 1f)
        val backProgress = playerSheet?.backProgressState?.floatValue ?: 0f
        clip = true
        shape = ClippedShape(
            peekHeight - 8.dp,
            positiveProgress,
            backProgress,
            8.dp,
            animatedStart.value,
            animatedEnd.value
        )
    }.run {
        if (colored) background(colorScheme.primaryContainer) else this
    }
}


fun formatTime(ms: Float): String {
    val totalSeconds = (ms / 1000).toInt().coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Composable
fun TimelineWithVolume(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.widthIn(256.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Timeline(Modifier.weight(1f))
        VolumeAdjuster()
    }
}

@Composable
fun Timeline(modifier: Modifier = Modifier) {
    val mergedStyle = LocalTextStyle.current.merge(typography.labelLarge)

    val timelineState = LocalPlayerTimelineState.current ?: return
    val maxRange = timelineState.durationMs
    var showRemainingTime by remember { mutableStateOf(false) }
    val currentTimeLabel by remember {
        derivedStateOf { formatTime(timelineState.positionMs) }
    }

    val endLabel by remember(showRemainingTime) {
        derivedStateOf {
            if (showRemainingTime) {
                "-${formatTime(maxRange - timelineState.positionMs)}"
            } else {
                formatTime(maxRange)
            }
        }
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = currentTimeLabel,
            style = mergedStyle,
            modifier = Modifier.widthIn(min = 40.dp),
            textAlign = TextAlign.Center
        )

        PlayerSlider(
            modifier = Modifier
                .weight(1f)
                .height(32.dp),
            timelineState = timelineState,
        )

        val endInteraction = remember { MutableInteractionSource() }
        Text(
            text = endLabel,
            style = mergedStyle,
            modifier = Modifier
                .widthIn(min = 40.dp)
                .clickable(interactionSource = endInteraction) {
                    showRemainingTime = !showRemainingTime
                },
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlayerSlider(
    modifier: Modifier = Modifier,
    timelineState: PlayerTimelineState,
) {
    val rangeMS = remember(timelineState.durationMs) { 0f..timelineState.durationMs }
    val segmentGaps = remember(timelineState.durationMs) {
        listOf(0.22f, 0.48f, 0.72f, 0.88f).map { fraction ->
            timelineState.durationMs * fraction
        }
    }
    val controls = LocalPlayerControls.current
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isDragged) {
        timelineState.isSeeking = isDragged
    }

    SquigglySlider(
        valueRange = rangeMS,
        value = { timelineState.positionMs },
        onValueChange = { newValue ->
            timelineState.isSeeking = true
            timelineState.positionMs = newValue
        },
        onValueChangeFinished = {
            timelineState.isSeeking = false
        },
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        interactionSource = interactionSource,
        segmentGaps = segmentGaps,
        segmentGapWidth = 3.dp,
        squiggleAmplitude = if (controls?.isPlaying != false) 0.66f else 0f,
        squiggleWavelength = 40.dp,
        waveSpeed = 40.dp,
        trackHeight = 16.dp,
        trackStrokeWidth = 4.dp,
        draggedTrackStrokeWidth = 12.dp,
        trackCornerSize = Dp.Unspecified,
        trackInsideCornerSize = 2.dp,
        stopIndicatorSize = 3.dp,
        thumbSize = DpSize(4.dp, 32.dp),
        thumbTrackGap = 3.dp,
        activeColor = colorScheme.primary,
        inactiveColor = colorScheme.primary.copy(alpha = 0.25f),
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VolumeAdjuster() {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState()
    val hasTouch = hasTouchInput.value
    val position = remember { mutableFloatStateOf(1f) }
    val lastAudiblePosition = remember { mutableFloatStateOf(1f) }
    var isSliderPinned by remember { mutableStateOf(false) }
    val sliderInteraction = remember { MutableInteractionSource() }
    val isSliderDragged = sliderInteraction.collectIsDraggedAsState()

    fun setVolume(value: Float) {
        val volume = value.coerceIn(0f, 1f)
        position.floatValue = volume
        if (volume > 0f) lastAudiblePosition.floatValue = volume
    }

    fun toggleMute() {
        if (position.floatValue > 0f) {
            lastAudiblePosition.floatValue = position.floatValue
            position.floatValue = 0f
        } else {
            position.floatValue = lastAudiblePosition.floatValue.coerceAtLeast(0.01f)
        }
    }

    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(100))
            .hoverable(interactionSource)
            .onPointerScrollY { delta ->
                val newVolume = position.floatValue - delta * 0.05f
                setVolume(newVolume)
            }
            .background(colorScheme.primary.copy(0.25f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(isHovered.value || isSliderDragged.value || (hasTouch && isSliderPinned)) {
            SquigglySlider(
                value = { position.floatValue },
                modifier = Modifier
                    .width(96.dp)
                    .height(40.dp)
                    .padding(start = 12.dp, end = 4.dp)
                    .pointerHoverIcon(PointerIcon.Hand),
                interactionSource = sliderInteraction,
                onValueChange = ::setVolume,
                squiggleAmplitude = 0f,
                trackStrokeWidth = 4.dp,
                draggedTrackStrokeWidth = 8.dp,
                thumbSize = DpSize(4.dp, 28.dp),
                activeColor = colorScheme.primary,
                inactiveColor = colorScheme.primary.copy(0.25f),
            )
        }
        IconButton(
            onClick = {
                if (hasTouch) isSliderPinned = !isSliderPinned
                else toggleMute()
            },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painterResource(
                    if (position.floatValue == 0f) Res.drawable.ic_volume_off
                    else Res.drawable.ic_volume_up,
                ),
                contentDescription = if (hasTouch) {
                    if (isSliderPinned) "Unpin volume slider" else "Pin volume slider"
                } else {
                    if (position.floatValue == 0f) "Unmute" else "Mute"
                },
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
