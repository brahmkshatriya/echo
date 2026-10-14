package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.motionScheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.platform.hasTouchInput
import dev.brahmkshatriya.echo.app.ui.components.MorphingProgressIconButton
import dev.brahmkshatriya.echo.app.ui.components.SquigglySlider
import dev.brahmkshatriya.echo.app.ui.components.SwitchingIcon
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerControls
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSkipNavigator
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerTimelineState
import dev.brahmkshatriya.echo.app.ui.player.PlayerTimelineState
import dev.brahmkshatriya.echo.app.ui.player.RepeatMode
import echo.client.generated.resources.Res
import echo.client.generated.resources.ic_favorite
import echo.client.generated.resources.ic_favorite_filled
import echo.client.generated.resources.ic_more_vert
import echo.client.generated.resources.ic_pause
import echo.client.generated.resources.ic_pause_32
import echo.client.generated.resources.ic_play_arrow
import echo.client.generated.resources.ic_play_arrow_32
import echo.client.generated.resources.ic_repeat
import echo.client.generated.resources.ic_repeat_on
import echo.client.generated.resources.ic_repeat_one_on
import echo.client.generated.resources.ic_skip_next
import echo.client.generated.resources.ic_skip_next_32
import echo.client.generated.resources.ic_skip_previous
import echo.client.generated.resources.ic_skip_previous_32
import echo.client.generated.resources.ic_volume_off
import echo.client.generated.resources.ic_volume_up
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

@Composable
fun defaultIconButtonColors() = IconButtonDefaults.iconButtonColors(
    contentColor = colorScheme.onPrimaryContainer,
    containerColor = colorScheme.onPrimaryContainer.copy(0.1f),
    disabledContainerColor = colorScheme.onPrimaryContainer.copy(0.05f),
    disabledContentColor = colorScheme.onPrimaryContainer.copy(0.5f)
)

@Composable
internal fun favouriteButtonColors() = colorScheme.primaryContainer
    .favouriteButtonPalette(
        dark = colorScheme.background.luminance() < 0.20f,
        artworkAccent = LocalPlayerFavouriteAccent.current,
    )
    .let { (checked, onChecked) ->
        IconButtonDefaults.filledTonalIconToggleButtonColors(
            colorScheme.onPrimaryContainer.copy(0.1f),
            colorScheme.onPrimaryContainer,
            checkedContainerColor = checked,
            checkedContentColor = onChecked,
        )
    }

@Composable
internal fun RepeatButton(modifier: Modifier = Modifier) {
    val controls = LocalPlayerControls.current
    val mode = controls?.repeatMode ?: RepeatMode.Off
    IconButton(
        onClick = {
            controls?.repeatMode = when (mode) {
                RepeatMode.Off -> RepeatMode.All
                RepeatMode.All -> RepeatMode.One
                RepeatMode.One -> RepeatMode.Off
            }
        },
        enabled = controls != null,
        modifier = modifier,
        shapes = IconButtonDefaults.shapes(),
        colors = defaultIconButtonColors()
    ) {
        SwitchingIcon(
            icon = when (mode) {
                RepeatMode.Off -> Res.drawable.ic_repeat
                RepeatMode.All -> Res.drawable.ic_repeat_on
                RepeatMode.One -> Res.drawable.ic_repeat_one_on
            },
            contentDescription = when (mode) {
                RepeatMode.Off -> "Repeat off; enable repeat all"
                RepeatMode.All -> "Repeat all; enable repeat one"
                RepeatMode.One -> "Repeat one; disable repeat"
            },
        )
    }
}

@Composable
internal fun PreviousButton(
    modifier: Modifier = Modifier,
    large: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    shapes: IconButtonShapes? = null,
) {
    val timelineState = LocalPlayerTimelineState.current
    val navigator = LocalPlayerSkipNavigator.current
    IconButton(
        onClick = {
            val timeline = timelineState ?: return@IconButton
            if (timeline.positionMs > 3_000f && navigator?.isNavigating != true) {
                timeline.positionMs = 0f
            } else {
                timeline.positionMs = 0f
                navigator?.previous()
            }
        },
        enabled = timelineState != null,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = colorScheme.onTertiary,
            containerColor = colorScheme.tertiary,
            disabledContainerColor = colorScheme.tertiary.copy(0.33f),
            disabledContentColor = colorScheme.onTertiary.copy(0.5f)
        ),
        shapes = shapes ?: IconButtonDefaults.shapes(),
        interactionSource = interactionSource,
    ) {
        SwitchingIcon(
            icon = if (large) Res.drawable.ic_skip_previous_32
            else Res.drawable.ic_skip_previous,
            contentDescription = "Previous song"
        )
    }
}

@Composable
internal fun PlayPauseButton(
    modifier: Modifier = Modifier,
    large: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    shapes: IconButtonShapes? = null,
) {
    val controls = LocalPlayerControls.current
    val isPlaying = controls?.isPlaying == true
    FilledIconButton(
        onClick = { controls?.isPlaying = !isPlaying },
        enabled = controls != null,
        modifier = modifier,
        shapes = shapes ?: IconButtonDefaults.shapes(
            shape = IconButtonDefaults.filledShape,
            pressedShape = IconButtonDefaults.filledShape,
        ),
        interactionSource = interactionSource,
        colors = IconButtonDefaults.filledIconButtonColors(
            colorScheme.primary,
            colorScheme.onPrimary
        )
    ) {
        SwitchingIcon(
            icon = when {
                isPlaying && large -> Res.drawable.ic_pause_32
                isPlaying -> Res.drawable.ic_pause
                large -> Res.drawable.ic_play_arrow_32
                else -> Res.drawable.ic_play_arrow
            },
            contentDescription = if (isPlaying) "Pause" else "Play"
        )
    }
}

@Composable
internal fun CompactPlayPauseButton(modifier: Modifier = Modifier) {
    val controls = LocalPlayerControls.current
    val isPlaying = controls?.isPlaying == true
    val timelineState = LocalPlayerTimelineState.current
    val progress = if (timelineState == null || timelineState.durationMs <= 0f) {
        0f
    } else {
        (timelineState.positionMs / timelineState.durationMs).coerceIn(0f, 1f)
    }
    val enabled = controls != null
    MorphingProgressIconButton(
        progress = progress,
        onClick = { controls?.isPlaying = !isPlaying },
        enabled = enabled,
        backgroundColor = colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
        progressColor = colorScheme.primary,
        buttonColor = if (enabled) colorScheme.primary else colorScheme.onSurface.copy(0.1f),
        modifier = modifier,
    ) {
        SwitchingIcon(
            icon = if (isPlaying) Res.drawable.ic_pause else Res.drawable.ic_play_arrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = if (enabled) colorScheme.onPrimary else colorScheme.onSurface.copy(0.38f),
        )
    }
}

@Composable
internal fun NextButton(
    modifier: Modifier = Modifier,
    large: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    shapes: IconButtonShapes? = null,
) {
    val timelineState = LocalPlayerTimelineState.current
    val navigator = LocalPlayerSkipNavigator.current
    val canGoNext = navigator?.canGoNext == true
    FilledTonalIconButton(
        onClick = {
            if (!canGoNext) return@FilledTonalIconButton
            timelineState?.positionMs = 0f
            navigator.next()
        },
        enabled = timelineState != null && canGoNext,
        modifier = modifier,
        shapes = shapes ?: IconButtonDefaults.shapes(),
        interactionSource = interactionSource,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = colorScheme.onTertiary,
            containerColor = colorScheme.tertiary,
            disabledContainerColor = colorScheme.tertiary.copy(0.33f),
            disabledContentColor = colorScheme.onTertiary.copy(0.5f)
        )
    ) {
        SwitchingIcon(
            icon = if (large) Res.drawable.ic_skip_next_32 else Res.drawable.ic_skip_next,
            contentDescription = "Next song"
        )
    }
}

@Composable
fun Controller() = BoxWithConstraints(Modifier.fillMaxWidth()) {
    val expandedShapes = IconButtonDefaults.shapes(
        shape = RoundedCornerShape(50),
        pressedShape = RoundedCornerShape(16.dp),
    )
    val previousSource = remember { MutableInteractionSource() }
    val playSource = remember { MutableInteractionSource() }
    val nextSource = remember { MutableInteractionSource() }
    val previousPressed by previousSource.collectIsPressedAsState()
    val playPressed by playSource.collectIsPressedAsState()
    val nextPressed by nextSource.collectIsPressedAsState()
    val pressedButton = when {
        previousPressed -> 0
        playPressed -> 1
        nextPressed -> 2
        else -> -1
    }
    val playWidth = (maxWidth - 32.dp - 56.dp * 2 - 8.dp * 2)
        .coerceIn(64.dp, 256.dp)
    val previousWidth by animateDpAsState(
        targetValue = when (pressedButton) {
            0 -> 64.dp
            1 -> 48.dp
            else -> 56.dp
        },
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "Previous button width",
    )
    val centerWidth by animateDpAsState(
        targetValue = when (pressedButton) {
            0, 2 -> playWidth - 8.dp
            1 -> playWidth + 16.dp
            else -> playWidth
        },
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "Play button width",
    )
    val nextWidth by animateDpAsState(
        targetValue = when (pressedButton) {
            2 -> 64.dp
            1 -> 48.dp
            else -> 56.dp
        },
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "Next button width",
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.align(Alignment.Center)
            .padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 4.dp)
    ) {
        PreviousButton(
            modifier = Modifier.width(previousWidth).height(64.dp),
            large = true,
            interactionSource = previousSource,
            shapes = expandedShapes,
        )
        PlayPauseButton(
            modifier = Modifier.width(centerWidth).height(64.dp),
            large = true,
            interactionSource = playSource,
            shapes = expandedShapes,
        )
        NextButton(
            modifier = Modifier.width(nextWidth).height(64.dp),
            large = true,
            interactionSource = nextSource,
            shapes = expandedShapes,
        )
    }
}

@Composable
fun ExpandedTimeline(
    i: Int,
    lyricsVisible: Boolean = false,
    showCompactArtwork: Boolean = false,
    rowsCollapseProgress: Float = 0f,
    onArtworkClick: () -> Unit = {},
) {
    val timelineState = LocalPlayerTimelineState.current ?: return
    val song = exampleSong(i)
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
    val coverSlideSpec = motionScheme.defaultSpatialSpec<IntOffset>()
    val coverWidthSpec = motionScheme.defaultSpatialSpec<IntSize>()

    SubcomposeLayout(Modifier.fillMaxWidth().clipToBounds()) { constraints ->
        val looseConstraints = constraints.copy(minHeight = 0)
        val metadata = subcompose("metadata") {
            Row(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(
                    visible = lyricsVisible || showCompactArtwork,
                    enter = slideInHorizontally(coverSlideSpec) { -it } +
                            expandHorizontally(coverWidthSpec, expandFrom = Alignment.Start),
                    exit = slideOutHorizontally(coverSlideSpec) { -it } +
                            shrinkHorizontally(coverWidthSpec, shrinkTowards = Alignment.Start),
                ) {
                    PlayerArtwork(
                        contentDescription = "${song?.album ?: "Album"} artwork",
                        modifier = Modifier
                            .padding(start = 16.dp, end = 8.dp)
                            .size(48.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colorScheme.primaryFixed)
                            .clickable(onClick = onArtworkClick),
                    )
                }
                AnimatedVisibility(
                    visible = !lyricsVisible && !showCompactArtwork,
                    enter = expandHorizontally(coverWidthSpec),
                    exit = shrinkHorizontally(coverWidthSpec),
                ) {
                    Spacer(Modifier.width(16.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(song?.title ?: "Unknown track", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        song?.artist ?: "Unknown artist",
                        color = colorScheme.onPrimaryContainer.copy(0.66f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(4.dp))
                LikeToggle(i)
                AnimatedVisibility(compactPlayPauseVisible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(4.dp))
                        CompactPlayPauseButton()
                    }
                }
                Spacer(Modifier.width(4.dp))
                MoreButton()
                Spacer(Modifier.width(16.dp))
            }
        }.single().measure(looseConstraints)

        val chromeTop = (metadata.height - 12.dp.roundToPx()).coerceAtLeast(0)
        val chrome = subcompose("chrome") {
            Layout(
                modifier = Modifier.fillMaxWidth(),
                content = {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = currentTimeLabel,
                            style = mergedStyle,
                            modifier = Modifier.padding(8.dp, 4.dp),
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
                                .padding(8.dp, 4.dp),
                        )
                    }
                    PlayerSlider(
                        modifier = Modifier.height(72.dp),
                        timelineState = timelineState,
                    )
                    Text(
                        text = "FLAC",
                        style = mergedStyle,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(100))
                            .background(colorScheme.onPrimaryContainer.copy(0.1f))
                            .clickable {}
                            .padding(12.dp, 4.dp),
                    )
                    Controller()
                },
            ) { measurables, chromeConstraints ->
                val chromeLoose = chromeConstraints.copy(minHeight = 0)
                val times = measurables[0].measure(chromeLoose)
                val sliderHorizontalPadding = 16.dp.roundToPx()
                val sliderWidth =
                    (chromeConstraints.maxWidth - sliderHorizontalPadding * 2).coerceAtLeast(0)
                val slider = measurables[1].measure(
                    Constraints(
                        minWidth = sliderWidth,
                        maxWidth = sliderWidth,
                        minHeight = 0,
                        maxHeight = chromeLoose.maxHeight,
                    )
                )
                val formatBadge = measurables[2].measure(chromeLoose.copy(minWidth = 0))
                val controls = measurables[3].measure(chromeLoose)
                val timesTop = metadata.height + 40.dp.roundToPx() - chromeTop
                val chromeContentHeight = maxOf(
                    timesTop + times.height,
                    slider.height,
                    formatBadge.height,
                )

                layout(chromeConstraints.maxWidth, chromeContentHeight + controls.height) {
                    slider.placeRelative(sliderHorizontalPadding, 0)
                    times.placeRelative(0, timesTop)
                    formatBadge.placeRelative(
                        (chromeConstraints.maxWidth - formatBadge.width) / 2,
                        chromeContentHeight - formatBadge.height,
                    )
                    controls.placeRelative(0, chromeContentHeight)
                }
            }
        }.single().measure(looseConstraints)

        val expandedContentHeight = chromeTop + chrome.height
        val collapsedContentHeight = metadata.height
        val contentHeight = (expandedContentHeight +
                (collapsedContentHeight - expandedContentHeight) * collapseProgress)
            .roundToInt().coerceIn(constraints.minHeight, constraints.maxHeight)
        val chromeTravel = (expandedContentHeight - collapsedContentHeight) * collapseProgress

        layout(constraints.maxWidth, contentHeight) {
            chrome.placeRelativeWithLayer(0, chromeTop) {
                alpha = secondaryRowsAlpha
                translationY = chromeTravel
            }
            metadata.placeRelative(0, 0)
        }
    }
}

@Composable
fun MoreButton() {
    IconButton(
        onClick = { },
        modifier = Modifier.height(44.dp).width(32.dp),
        shapes = IconButtonDefaults.shapes(),
        colors = defaultIconButtonColors()
    ) {
        Icon(
            painterResource(Res.drawable.ic_more_vert),
            contentDescription = "Close Player"
        )
    }
}

@Composable
fun LikeToggle(song: Int) {
    val controls = LocalPlayerControls.current
    val favourite = controls?.isFavourite(song) == true
    val interactionSource = remember { MutableInteractionSource() }
    FilledTonalIconToggleButton(
        checked = favourite,
        onCheckedChange = { controls?.setFavourite(song, it) },
        enabled = controls != null,
        modifier = Modifier.size(44.dp),
        shapes = IconButtonDefaults.toggleableShapes(
            checkedShape = RoundedCornerShape(100)
        ),
        interactionSource = interactionSource,
        colors = favouriteButtonColors(),
    ) {
        SwitchingIcon(
            icon = if (favourite) Res.drawable.ic_favorite_filled
            else Res.drawable.ic_favorite,
            contentDescription = if (favourite) "Unfavourite" else "Favourite",
            modifier = Modifier.padding(top = 2.dp)
        )
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
            modifier = Modifier.widthIn(min = 40.dp) .padding(8.dp, 4.dp),
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
                .clip(RoundedCornerShape(8.dp))
                .clickable(interactionSource = endInteraction) {
                    showRemainingTime = !showRemainingTime
                }
                .padding(8.dp, 4.dp),
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
        thumbSize = DpSize(4.dp, 24.dp),
        activeColor = colorScheme.primary,
        inactiveColor = colorScheme.onPrimaryContainer.copy(alpha = 0.05f),
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
                val newVolume = position.floatValue - delta * 0.0015f
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
                thumbSize = DpSize(4.dp, 20.dp),
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
            SwitchingIcon(
                icon = if (position.floatValue == 0f) Res.drawable.ic_volume_off
                else Res.drawable.ic_volume_up,
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
