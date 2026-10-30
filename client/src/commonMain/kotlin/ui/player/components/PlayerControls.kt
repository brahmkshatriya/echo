package dev.brahmkshatriya.echo.app.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.platform.hasTouchInput
import dev.brahmkshatriya.echo.app.playback.PlayerTimelineState
import dev.brahmkshatriya.echo.app.playback.RepeatMode
import dev.brahmkshatriya.echo.app.ui.components.MorphingProgressIconButton
import dev.brahmkshatriya.echo.app.ui.components.SquigglySlider
import dev.brahmkshatriya.echo.app.ui.components.SwitchingIcon
import dev.brahmkshatriya.echo.app.ui.player.artwork.LocalPlayerFavouriteAccent
import dev.brahmkshatriya.echo.app.ui.player.artwork.favouriteButtonPalette
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

@Composable
fun defaultIconButtonColors() =
    IconButtonDefaults.iconButtonColors(
        contentColor = colorScheme.onPrimaryContainer,
        containerColor = colorScheme.onPrimaryContainer.copy(0.1f),
        disabledContainerColor = colorScheme.onPrimaryContainer.copy(0.05f),
        disabledContentColor = colorScheme.onPrimaryContainer.copy(0.5f),
    )

@Composable
fun favouriteButtonColors() =
    colorScheme.primaryContainer
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
fun RepeatButton(mode: RepeatMode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        shapes = IconButtonDefaults.shapes(),
        colors = defaultIconButtonColors(),
    ) {
        SwitchingIcon(
            icon =
                when (mode) {
                    RepeatMode.Off -> Res.drawable.ic_repeat
                    RepeatMode.All -> Res.drawable.ic_repeat_on
                    RepeatMode.One -> Res.drawable.ic_repeat_one_on
                },
            contentDescription =
                when (mode) {
                    RepeatMode.Off -> "Repeat off; enable repeat all"
                    RepeatMode.All -> "Repeat all; enable repeat one"
                    RepeatMode.One -> "Repeat one; disable repeat"
                },
        )
    }
}

@Composable
fun PreviousButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    shapes: IconButtonShapes? = null,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors =
            IconButtonDefaults.iconButtonColors(
                contentColor = colorScheme.onTertiary,
                containerColor = colorScheme.tertiary,
                disabledContainerColor = colorScheme.tertiary.copy(0.33f),
                disabledContentColor = colorScheme.onTertiary.copy(0.5f),
            ),
        shapes = shapes ?: IconButtonDefaults.shapes(),
        interactionSource = interactionSource,
    ) {
        SwitchingIcon(
            icon = if (large) Res.drawable.ic_skip_previous_32 else Res.drawable.ic_skip_previous,
            contentDescription = "Previous item",
        )
    }
}

@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    shapes: IconButtonShapes? = null,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier,
        shapes = shapes ?: IconButtonDefaults.shapes(),
        interactionSource = interactionSource,
        colors =
            IconButtonDefaults.filledIconButtonColors(colorScheme.primary, colorScheme.onPrimary),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLoading)
                CircularProgressIndicator(
                    modifier = Modifier.size(if (large) 48.dp else 34.dp),
                    color = colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            SwitchingIcon(
                icon =
                    when {
                        isPlaying && large -> Res.drawable.ic_pause_32
                        isPlaying -> Res.drawable.ic_pause
                        large -> Res.drawable.ic_play_arrow_32
                        else -> Res.drawable.ic_play_arrow
                    },
                contentDescription =
                    when {
                        isLoading -> "Song loading"
                        isPlaying -> "Pause"
                        else -> "Play"
                    },
            )
        }
    }
}

@Composable
fun CompactPlayPauseButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    timelineState: PlayerTimelineState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MorphingProgressIconButton(
        progress = {
            if (timelineState == null || timelineState.durationMs <= 0f) 0f
            else (timelineState.positionMs / timelineState.durationMs).coerceIn(0f, 1f)
        },
        indeterminate = isLoading,
        onClick = onClick,
        enabled = true,
        backgroundColor = colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
        progressColor = colorScheme.primary,
        buttonColor = colorScheme.primary,
        modifier = modifier,
    ) {
        SwitchingIcon(
            icon = if (isPlaying) Res.drawable.ic_pause else Res.drawable.ic_play_arrow,
            contentDescription =
                when {
                    isLoading -> "Song loading"
                    isPlaying -> "Pause"
                    else -> "Play"
                },
            tint = colorScheme.onPrimary,
        )
    }
}

@Composable
fun NextButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    shapes: IconButtonShapes? = null,
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shapes = shapes ?: IconButtonDefaults.shapes(),
        interactionSource = interactionSource,
        colors =
            IconButtonDefaults.iconButtonColors(
                contentColor = colorScheme.onTertiary,
                containerColor = colorScheme.tertiary,
                disabledContainerColor = colorScheme.tertiary.copy(0.33f),
                disabledContentColor = colorScheme.onTertiary.copy(0.5f),
            ),
    ) {
        SwitchingIcon(
            icon = if (large) Res.drawable.ic_skip_next_32 else Res.drawable.ic_skip_next,
            contentDescription = "Next item",
        )
    }
}

@Composable
fun Controller(
    isPlaying: Boolean,
    isLoading: Boolean,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) =
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val expandedShapes =
            IconButtonDefaults.shapes(
                shape = RoundedCornerShape(50),
                pressedShape = RoundedCornerShape(16.dp),
            )
        val previousSource = remember { MutableInteractionSource() }
        val playSource = remember { MutableInteractionSource() }
        val nextSource = remember { MutableInteractionSource() }
        val previousPressed by previousSource.collectIsPressedAsState()
        val playPressed by playSource.collectIsPressedAsState()
        val nextPressed by nextSource.collectIsPressedAsState()
        val pressedButton =
            when {
                previousPressed -> 0
                playPressed -> 1
                nextPressed -> 2
                else -> -1
            }
        val playWidth = (maxWidth - 32.dp - 56.dp * 2 - 8.dp * 2).coerceIn(64.dp, 256.dp)
        val previousWidth by
            animateDpAsState(
                targetValue =
                    when (pressedButton) {
                        0 -> 64.dp
                        1 -> 48.dp
                        else -> 56.dp
                    },
                animationSpec = motionScheme.fastSpatialSpec(),
                label = "Previous button width",
            )
        val centerWidth by
            animateDpAsState(
                targetValue =
                    when (pressedButton) {
                        0,
                        2 -> playWidth - 8.dp
                        1 -> playWidth + 16.dp
                        else -> playWidth
                    },
                animationSpec = motionScheme.fastSpatialSpec(),
                label = "Play button width",
            )
        val nextWidth by
            animateDpAsState(
                targetValue =
                    when (pressedButton) {
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
            modifier =
                Modifier.align(Alignment.Center)
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 8.dp),
        ) {
            PreviousButton(
                enabled = previousEnabled,
                onClick = onPrevious,
                modifier = Modifier.width(previousWidth).height(64.dp),
                large = true,
                interactionSource = previousSource,
                shapes = expandedShapes,
            )
            PlayPauseButton(
                isPlaying = isPlaying,
                isLoading = isLoading,
                onClick = onPlayPause,
                modifier = Modifier.width(centerWidth).height(64.dp),
                large = true,
                interactionSource = playSource,
                shapes = expandedShapes,
            )
            NextButton(
                enabled = nextEnabled,
                onClick = onNext,
                modifier = Modifier.width(nextWidth).height(64.dp),
                large = true,
                interactionSource = nextSource,
                shapes = expandedShapes,
            )
        }
    }

@Composable
fun MoreButton() {
    IconButton(
        onClick = {},
        modifier = Modifier.height(44.dp).width(32.dp),
        shapes = IconButtonDefaults.shapes(),
        colors = defaultIconButtonColors(),
    ) {
        Icon(painterResource(Res.drawable.ic_more_vert), contentDescription = "Close Player")
    }
}

@Composable
fun LikeToggle(favourite: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    FilledTonalIconToggleButton(
        checked = favourite,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.size(44.dp),
        shapes = IconButtonDefaults.toggleableShapes(checkedShape = RoundedCornerShape(100)),
        interactionSource = interactionSource,
        colors = favouriteButtonColors(),
    ) {
        SwitchingIcon(
            icon = if (favourite) Res.drawable.ic_favorite_filled else Res.drawable.ic_favorite,
            contentDescription = if (favourite) "Unfavourite" else "Favourite",
            modifier = Modifier.padding(top = 2.dp),
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
fun TimelineWithVolume(
    timelineState: PlayerTimelineState,
    isPlaying: Boolean,
    volume: FloatState,
    lastAudibleVolume: FloatState,
    onVolumeChange: (Float) -> Unit,
    onSeekPreview: (Float) -> Unit,
    onSeekCommit: () -> Unit,
    onSeekCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.widthIn(256.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Timeline(
            timelineState,
            isPlaying,
            onSeekPreview,
            onSeekCommit,
            onSeekCancel,
            Modifier.weight(1f),
        )
        VolumeAdjuster(volume, lastAudibleVolume, onVolumeChange)
    }
}

@Composable
fun Timeline(
    timelineState: PlayerTimelineState,
    isPlaying: Boolean,
    onSeekPreview: (Float) -> Unit,
    onSeekCommit: () -> Unit,
    onSeekCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mergedStyle = LocalTextStyle.current.merge(typography.labelLarge)

    var showRemainingTime by remember { mutableStateOf(false) }
    val timeLabels = rememberTimelineLabels(timelineState, showRemainingTime)
    val (currentTimeLabel, endLabel) = timeLabels.value
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = currentTimeLabel,
            style = mergedStyle,
            modifier = Modifier.widthIn(min = 40.dp).padding(8.dp, 4.dp),
            textAlign = TextAlign.Center,
        )

        PlayerSlider(
            modifier = Modifier.weight(1f).height(32.dp),
            timelineState = timelineState,
            isPlaying = isPlaying,
            onSeekPreview = onSeekPreview,
            onSeekCommit = onSeekCommit,
            onSeekCancel = onSeekCancel,
        )

        val endInteraction = remember { MutableInteractionSource() }
        Text(
            text = endLabel,
            style = mergedStyle,
            modifier =
                Modifier.widthIn(min = 40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(interactionSource = endInteraction) {
                        showRemainingTime = !showRemainingTime
                    }
                    .padding(8.dp, 4.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun PlayerSlider(
    modifier: Modifier = Modifier,
    timelineState: PlayerTimelineState,
    isPlaying: Boolean,
    onSeekPreview: (Float) -> Unit,
    onSeekCommit: () -> Unit,
    onSeekCancel: () -> Unit,
) {
    val rangeMS = remember(timelineState.durationMs) { 0f..timelineState.durationMs }
    val segmentGaps =
        remember(timelineState.durationMs) {
            listOf(0.22f, 0.48f, 0.72f, 0.88f).map { fraction ->
                timelineState.durationMs * fraction
            }
        }
    val interactionSource = remember(timelineState) { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val currentOnSeekCancel by rememberUpdatedState(onSeekCancel)
    LaunchedEffect(timelineState, isDragged) { if (!isDragged) currentOnSeekCancel() }
    androidx.compose.runtime.DisposableEffect(timelineState) { onDispose { onSeekCancel() } }

    SquigglySlider(
        enabled = timelineState.durationMs > 0f,
        valueRange = rangeMS,
        value = { timelineState.positionMs },
        bufferedValue = { timelineState.bufferedMs },
        onValueChange = onSeekPreview,
        onValueChangeFinished = onSeekCommit,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        interactionSource = interactionSource,
        segmentGaps = segmentGaps,
        segmentGapWidth = 3.dp,
        squiggleAmplitude = if (isPlaying) 0.66f else 0f,
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
        bufferedColor = colorScheme.primary.copy(alpha = 0.35f),
    )
}

@Composable
fun VolumeAdjuster(
    position: FloatState,
    lastAudiblePosition: FloatState,
    onVolumeChange: (Float) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState()
    val hasTouch = hasTouchInput.value
    var isSliderPinned by remember { mutableStateOf(false) }
    val sliderInteraction = remember { MutableInteractionSource() }
    val isSliderDragged = sliderInteraction.collectIsDraggedAsState()

    fun setVolume(value: Float) = onVolumeChange(value)
    fun toggleMute() =
        onVolumeChange(
            if (position.floatValue > 0f) 0f
            else lastAudiblePosition.floatValue.coerceAtLeast(0.01f)
        )

    Row(
        modifier =
            Modifier.height(40.dp)
                .clip(RoundedCornerShape(100))
                .hoverable(interactionSource)
                .onPointerScrollY { delta ->
                    val newVolume = position.floatValue - delta * 0.0015f
                    setVolume(newVolume)
                }
                .background(colorScheme.primary.copy(0.25f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            isHovered.value || isSliderDragged.value || (hasTouch && isSliderPinned)
        ) {
            SquigglySlider(
                value = { position.floatValue },
                modifier =
                    Modifier.width(96.dp)
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
            onClick = { if (hasTouch) isSliderPinned = !isSliderPinned else toggleMute() },
            modifier = Modifier.size(40.dp),
        ) {
            SwitchingIcon(
                icon =
                    if (position.floatValue == 0f) Res.drawable.ic_volume_off
                    else Res.drawable.ic_volume_up,
                contentDescription =
                    if (hasTouch) {
                        if (isSliderPinned) "Unpin volume slider" else "Pin volume slider"
                    } else {
                        if (position.floatValue == 0f) "Unmute" else "Mute"
                    },
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
