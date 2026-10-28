package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.playback.LocalPlayerTimelineState
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerControls
import dev.brahmkshatriya.echo.app.ui.player.artwork.PlayerArtwork
import dev.brahmkshatriya.echo.app.ui.player.components.CompactPlayPauseButton
import dev.brahmkshatriya.echo.app.ui.player.components.Controller
import dev.brahmkshatriya.echo.app.ui.player.components.LikeToggle
import dev.brahmkshatriya.echo.app.ui.player.components.MoreButton
import dev.brahmkshatriya.echo.app.ui.player.components.PlayerSlider
import dev.brahmkshatriya.echo.app.ui.player.components.formatTime

@Composable
internal fun ExpandedTimeline(
    i: Int,
    geometry: ExpandedTimelineGeometry,
    rowsCollapsed: Boolean,
    lyricsVisible: Boolean = false,
    showCompactArtwork: Boolean = false,
    rowsCollapseProgress: () -> Float = { 0f },
    onArtworkClick: () -> Unit = {},
) {
    val timelineState = LocalPlayerTimelineState.current ?: return
    val song = LocalPlayerControls.current.mediaFor(i)
    val maxRange = timelineState.durationMs
    var showRemainingTime by remember { mutableStateOf(false) }
    fun labelSeconds(): Pair<Int, Int> {
        val positionMs = timelineState.positionMs
        return (positionMs / 1_000).toInt().coerceAtLeast(0) to
                ((if (showRemainingTime) maxRange - positionMs else maxRange) / 1_000)
                    .toInt().coerceAtLeast(0)
    }
    fun labels(seconds: Pair<Int, Int>): Pair<String, String> =
        formatTime(seconds.first * 1_000f) to
                (if (showRemainingTime) "-" else "") + formatTime(seconds.second * 1_000f)

    val timeLabels by produceState(
        initialValue = Snapshot.withoutReadObservation { labels(labelSeconds()) },
        timelineState, showRemainingTime,
    ) {
        // Publish text only when a displayed second changes. Observing a
        // derived string in composition still revisits this group every tick.
        snapshotFlow { labelSeconds() }.collect { value = labels(it) }
    }
    val (currentTimeLabel, endLabel) = timeLabels
    val mergedStyle = LocalTextStyle.current.merge(typography.labelLarge)
    val compactPlayPauseVisible = lyricsVisible && rowsCollapsed
    val coverSlideSpec = motionScheme.defaultSpatialSpec<IntOffset>()
    val coverWidthSpec = motionScheme.defaultSpatialSpec<IntSize>()

    Layout(
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            clip = true
            shape = PlayerViewportShape(geometry.visibleHeightPx(rowsCollapseProgress()))
        },
        content = {
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
                    Text(
                        song?.title ?: "Unknown track",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
            Layout(
                modifier = Modifier.fillMaxWidth(),
                content = {
                    Box(Modifier.padding(horizontal = 12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = currentTimeLabel,
                                style = mergedStyle,
                                modifier = Modifier.padding(8.dp, 4.dp),
                            )
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
                        Text(
                            text = song?.uri?.substringAfterLast('.')?.uppercase() ?: "Audio",
                            style = mergedStyle,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(100))
                                .background(colorScheme.onPrimaryContainer.copy(0.1f))
                                .clickable {}
                                .padding(12.dp, 4.dp),
                        )
                    }
                    PlayerSlider(
                        modifier = Modifier.height(72.dp),
                        timelineState = timelineState,
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
                val controls = measurables[2].measure(chromeLoose)
                // Metadata is at least 48.dp high. Its height cancels out
                // against the chrome's 12.dp overlap, so composition does not
                // need to run from the parent's measurement pass.
                val timesTop = 40.dp.roundToPx() + 12.dp.roundToPx()
                val chromeContentHeight = maxOf(
                    timesTop + times.height,
                    slider.height,
                )

                layout(chromeConstraints.maxWidth, chromeContentHeight + controls.height) {
                    slider.placeRelative(sliderHorizontalPadding, 0)
                    times.placeRelative(0, timesTop)
                    controls.placeRelative(0, chromeContentHeight)
                }
            }
        },
    ) { measurables, constraints ->
        val looseConstraints = constraints.copy(minHeight = 0)
        val metadata = measurables[0].measure(looseConstraints)
        val chrome = measurables[1].measure(looseConstraints)
        val chromeTop = (metadata.height - 12.dp.roundToPx()).coerceAtLeast(0)
        val expandedContentHeight = chromeTop + chrome.height
        geometry.metadataHeightPx = metadata.height
        geometry.expandedHeightPx = expandedContentHeight

        // The parent moves the measured timeline. Only layer properties read
        // animation progress, so the controls and lyric viewport keep their size.
        layout(constraints.maxWidth, expandedContentHeight.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            chrome.placeRelativeWithLayer(0, chromeTop) {
                val collapseProgress = rowsCollapseProgress().coerceIn(0f, 1f)
                alpha = 1f - collapseProgress
                translationY = geometry.secondaryHeightPx * collapseProgress
            }
            metadata.placeRelative(0, 0)
        }
    }
}
