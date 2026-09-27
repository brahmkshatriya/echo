package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialShapes.Companion.Circle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.platform.hasTouchInput
import dev.brahmkshatriya.echo.app.ui.components.BetterImage
import dev.brahmkshatriya.echo.app.ui.components.ResponsiveRow
import dev.brahmkshatriya.echo.app.ui.player.CompactPlayPauseButton
import dev.brahmkshatriya.echo.app.ui.player.Controller
import dev.brahmkshatriya.echo.app.ui.player.ExpandedTimeline
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerPadding
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSheet
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerTimelineState
import dev.brahmkshatriya.echo.app.ui.player.NextButton
import dev.brahmkshatriya.echo.app.ui.player.PlayPauseButton
import dev.brahmkshatriya.echo.app.ui.player.PlayerArtwork
import dev.brahmkshatriya.echo.app.ui.player.PreviousButton
import dev.brahmkshatriya.echo.app.ui.player.RepeatButton
import dev.brahmkshatriya.echo.app.ui.player.ShuffleButton
import dev.brahmkshatriya.echo.app.ui.player.TimelineWithVolume
import dev.brahmkshatriya.echo.app.ui.player.collapsedHorizontalPadding
import dev.brahmkshatriya.echo.app.ui.player.playerBottomBarControlSize
import dev.brahmkshatriya.echo.app.ui.player.playerBottomBarInset
import dev.brahmkshatriya.echo.app.ui.player.playerBottomBarItemSpacing
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.Lyrics
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsPanel
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsSelectionItem
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsSelectionPillContent
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsTicker
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsTimingIndex
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.NoLyricsTicker
import echo.client.generated.resources.Res
import echo.client.generated.resources.ic_close
import echo.client.generated.resources.ic_favorite
import echo.client.generated.resources.ic_favorite_filled
import echo.client.generated.resources.ic_keyboard_arrow_down
import echo.client.generated.resources.ic_keyboard_arrow_up
import echo.client.generated.resources.ic_lyrics_mic
import echo.client.generated.resources.ic_lyrics_mic_off
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private enum class PlayerHeroSlot {
    TopBar,
    CoverOrLyrics,
    Timeline,
    Controller,
}

@Composable
internal fun PlayerHero(
    i: Int,
    lyrics: Lyrics?,
    lyricsTimingIndex: LyricsTimingIndex?,
    showLyrics: Boolean,
    collapseTimelineRowsForLyrics: Boolean,
    onCollapseTimelineRowsForLyricsChange: (Boolean) -> Unit,
    onLyricsChromeInteraction: () -> Unit,
    userScrollEnabled: Boolean,
    topPadding: Dp,
    viewportWidth: Dp,
    transformModifier: Modifier,
    onArtworkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chromeCollapseProgress by animateFloatAsState(
        targetValue = if (collapseTimelineRowsForLyrics) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "Lyrics player chrome collapse",
    )
    LaunchedEffect(showLyrics) {
        if (!showLyrics && collapseTimelineRowsForLyrics) {
            delay(LyricsModeTransitionDurationMs.toLong().milliseconds)
            onCollapseTimelineRowsForLyricsChange(false)
        }
    }

    val currentOnLyricsChromeInteraction by rememberUpdatedState(onLyricsChromeInteraction)
    val chromeInteractionModifier = if (showLyrics) {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                var consumedByDescendant: Boolean
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Final,
                )
                consumedByDescendant = down.isConsumed
                var anyPressed = true
                while (anyPressed) {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    if (event.changes.any { it.isConsumed }) consumedByDescendant = true
                    anyPressed = event.changes.any { it.pressed }
                }
                if (!consumedByDescendant) currentOnLyricsChromeInteraction()
            }
        }
    } else Modifier

    SubcomposeLayout(
        modifier.then(if (showLyrics) Modifier.clipToBounds() else Modifier)
    ) { constraints ->
        val childConstraints = constraints.copy(minHeight = 0)
        val topBar = subcompose(PlayerHeroSlot.TopBar) {
            TopBar(i, transformModifier)
        }.single().measure(childConstraints)
        val timeline = subcompose(PlayerHeroSlot.Timeline) {
            Box(transformModifier.then(chromeInteractionModifier)) {
                ExpandedTimeline(
                    i = i,
                    lyricsVisible = showLyrics,
                    rowsCollapseProgress = chromeCollapseProgress,
                    onArtworkClick = onArtworkClick,
                )
            }
        }.single().measure(childConstraints)
        val controller = subcompose(PlayerHeroSlot.Controller) {
            Box(
                transformModifier
                    .then(chromeInteractionModifier)
                    .graphicsLayer {
                        alpha = 1f - chromeCollapseProgress
                    }
            ) {
                Controller()
            }
        }.single().measure(childConstraints)
        val controllerVisibleHeight = (controller.height * (1f - chromeCollapseProgress))
            .roundToInt().coerceAtLeast(0)

        val coverHeight = (
                constraints.maxHeight - topBar.height - timeline.height - controllerVisibleHeight
                ).coerceAtLeast(0)
        val coverHeightDp = coverHeight.toDp()
        val coverMaxSize = minOf(
            maxSongCoverSize.dp,
            (viewportWidth - (songCoverHorizontalPadding * 2).dp).coerceAtLeast(0.dp),
            (coverHeightDp - (songCoverVerticalPadding * 2).dp).coerceAtLeast(0.dp),
        )
        val coverVerticalPadding = ((coverHeightDp - coverMaxSize) / 2)
            .coerceAtLeast(songCoverVerticalPadding.dp)
        val cover = subcompose(PlayerHeroSlot.CoverOrLyrics) {
            AnimatedContent(
                targetState = showLyrics,
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    val animation = tween<Float>(
                        LyricsModeTransitionDurationMs,
                        easing = FastOutSlowInEasing,
                    )
                    val enter = fadeIn(animation) + scaleIn(animation, 0.96f)
                    val exit = fadeOut(tween(LyricsModeTransitionDurationMs / 2)) +
                            scaleOut(animation, 1.04f)
                    enter togetherWith exit
                },
            ) { lyricsVisible ->
                if (lyricsVisible) {
                    if (lyrics != null) {
                        LyricsPanel(
                            lyrics = lyrics,
                            timingIndex = lyricsTimingIndex,
                            userScrollEnabled = userScrollEnabled,
                            chromeCollapsed = collapseTimelineRowsForLyrics,
                            onChromeInteraction = currentOnLyricsChromeInteraction,
                            onViewportMotionChange = { moving ->
                                onCollapseTimelineRowsForLyricsChange(moving)
                            },
                            modifier = transformModifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = transformModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No Lyrics",
                                style = typography.headlineSmall,
                                color = colorScheme.primary,
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        CoverArt(
                            i = i,
                            maxCoverSize = coverMaxSize,
                            verticalPadding = coverVerticalPadding,
                            topPadding = topPadding,
                            viewportWidth = viewportWidth,
                        )
                    }
                }
            }
        }.single().measure(
            childConstraints.copy(
                minHeight = coverHeight,
                maxHeight = coverHeight,
            )
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            var y = 0
            topBar.placeRelative(0, y)
            y += topBar.height
            cover.placeRelative(0, y)
            y += cover.height
            timeline.placeRelative(0, y)
            y += timeline.height
            controller.placeRelative(0, y)
        }
    }
}

@Composable
fun CoverArt(
    i: Int,
    maxCoverSize: Dp,
    verticalPadding: Dp,
    topPadding: Dp,
    viewportWidth: Dp,
) {
    val playerSheet = LocalPlayerSheet.current
    val playerPadding = LocalPlayerPadding.current

    val layoutDirection = LocalLayoutDirection.current
    val animatedTargetX = animateDpAsState(
        playerPadding.calculateStartPadding(layoutDirection) + (collapsedHorizontalPadding + 8).dp,
        tween()
    )
    PlayerArtwork(
        contentDescription = "Song $i",
        modifier = Modifier
            .songCoverSize(maxCoverSize, verticalPadding)
            .graphicsLayer {
                val sheetProgress = playerSheet?.progressState?.floatValue ?: 0f
                val positiveProgress = sheetProgress.coerceIn(0f, 1f)
                val offset = 1 - positiveProgress

                val targetX = animatedTargetX.value.toPx()
                val targetY = -(playerSheet?.peekHeight?.toPx() ?: 0f) / 2
                val targetSize = 48.dp
                val targetScale = if (size.height > 0f) {
                    targetSize.toPx() / size.height
                } else {
                    1f
                }
                val coverScale = 1 + (targetScale - 1) * offset
                scaleX = coverScale
                scaleY = coverScale
                transformOrigin = TransformOrigin(0f, 0f)
                val center = (viewportWidth.toPx() - size.width) / 2f
                translationX =
                    -songCoverHorizontalPadding.dp.toPx() + targetX * offset + center * positiveProgress

                val collapsedY = targetY - verticalPadding.toPx() - topPadding.toPx()
                translationY = collapsedY * offset

                clip = true
                shape = RoundedCornerShape((8 / coverScale).dp)
            }
            .background(colorScheme.primaryFixed),
    )
}

@Composable
fun TopBar(
    index: Int,
    modifier: Modifier = Modifier,
) {
    val playerSheet = LocalPlayerSheet.current
    val sheetState = playerSheet?.sheetState
    val scope = rememberCoroutineScope()
    Row(modifier.padding(end = 8.dp)) {
        IconButton(
            onClick = {
                scope.launch { sheetState?.show() }
            }, shapes = IconButtonDefaults.shapes()
        ) {
            Icon(
                painterResource(Res.drawable.ic_keyboard_arrow_down),
                contentDescription = "Minimize Player"
            )
        }
        Column(
            Modifier.padding(start = 8.dp, top = 8.dp).weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val mergedStyle = LocalTextStyle.current.merge(typography.labelLarge)
            Text("Playing From", fontWeight = FontWeight.Normal, style = mergedStyle)
            Text("Album $index", fontWeight = FontWeight.Bold, style = mergedStyle)
        }
        IconButton(
            onClick = { },
            modifier = Modifier.padding(top = 8.dp),
            shapes = IconButtonDefaults.shapes()
        ) {
            BetterImage(
                model = { "https://play-lh.googleusercontent.com/7ynvVIRdhJNAngCg_GI7i8TtH8BqkJYmffeUHsG-mJOdzt1XLvGmbsKuc5Q1SInBjDKN" },
                "Spotify",
                modifier = Modifier.padding(4.dp).clip(Circle.toShape())
            )
        }
    }
}


@Composable
internal fun BottomBar(
    index: Int = 0,
    isSticky: Boolean = false,
    lyricsVisible: Boolean = false,
    selectedLyrics: LyricsSelectionItem? = null,
    lyricsSelectorActive: Boolean = false,
    onLyricsSelectorClick: () -> Unit = {},
    onLyricsPillBoundsChanged: (Rect) -> Unit = {},
    onLyricsClick: () -> Unit = {}
) {
    val stickyProgress by animateFloatAsState(
        if (isSticky) 1f else 0f,
        tween()
    )
    Box(
        Modifier
            .fillMaxWidth()
            .background(colorScheme.primaryContainer.copy(alpha = stickyProgress))
            .padding(playerBottomBarInset)
    ) {
        Crossfade(
            targetState = isSticky,
            animationSpec = tween()
        ) { sticky ->
            if (sticky) StickyMiniPlayer(index)
            else LyricsBottomBar(
                lyricsVisible = lyricsVisible,
                selectedLyrics = selectedLyrics,
                lyricsSelectorActive = lyricsSelectorActive,
                onLyricsSelectorClick = onLyricsSelectorClick,
                onLyricsPillBoundsChanged = onLyricsPillBoundsChanged,
                onLyricsClick = onLyricsClick,
            )
        }
    }
}

@Composable
private fun LyricsToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    FilledTonalIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = IconButtonDefaults.toggleableShapes(
            checkedShape = RoundedCornerShape(100),
        ),
        interactionSource = interactionSource,
        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
            Color.Transparent,
            colorScheme.onPrimaryContainer,
            checkedContainerColor = Color.Transparent,
            checkedContentColor = colorScheme.onPrimaryContainer,
        ),
    ) {
        Crossfade(
            targetState = checked,
            animationSpec = tween(LyricsTransitionDurationMs),
        ) { isChecked ->
            Icon(
                painterResource(
                    if (isChecked) Res.drawable.ic_lyrics_mic_off
                    else Res.drawable.ic_lyrics_mic,
                ),
                contentDescription = if (isChecked) "Hide lyrics" else "Show lyrics",
            )
        }
    }
}

@Composable
private fun LyricsBottomBar(
    lyricsVisible: Boolean,
    selectedLyrics: LyricsSelectionItem?,
    lyricsSelectorActive: Boolean,
    onLyricsSelectorClick: () -> Unit,
    onLyricsPillBoundsChanged: (Rect) -> Unit,
    onLyricsClick: () -> Unit
) {
    val timelineState = LocalPlayerTimelineState.current ?: return
    Row(
        Modifier.fillMaxWidth().height(playerBottomBarControlSize),
        horizontalArrangement = Arrangement.spacedBy(playerBottomBarItemSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LyricsToggle(
            checked = lyricsVisible,
            onCheckedChange = { onLyricsClick() }
        )
        Box(
            Modifier.height(playerBottomBarControlSize)
                .weight(1f)
                .onGloballyPositioned { onLyricsPillBoundsChanged(it.boundsInRoot()) }
                .graphicsLayer { alpha = if (lyricsSelectorActive) 0f else 1f }
                .clip(RoundedCornerShape(playerBottomBarControlSize / 2f))
                .background(
                    color = colorScheme.primary.copy(0.1f),
                    shape = RoundedCornerShape(playerBottomBarControlSize / 2f),
                )
                .clickable(
                    onClick = if (lyricsVisible) onLyricsSelectorClick else onLyricsClick
                )
        ) {
            Crossfade(
                targetState = lyricsVisible,
                animationSpec = tween(LyricsTransitionDurationMs)
            ) { modeVisible ->
                if (modeVisible) {
                    LyricsSelectionPillContent(selectedLyrics)
                } else if (selectedLyrics != null) LyricsTicker(
                    lyrics = selectedLyrics.lyrics,
                    timelineState = timelineState,
                ) else NoLyricsTicker()
            }
        }

        IconButton(
            onClick = { },
            shapes = IconButtonDefaults.shapes()
        ) {
            Icon(
                painterResource(Res.drawable.ic_keyboard_arrow_up),
                contentDescription = "Scroll to Top"
            )
        }
    }
}


@Composable
private fun StickyMiniPlayer(
    index: Int,
) {
    val playerSheet = LocalPlayerSheet.current
    val sheetState = playerSheet?.sheetState
    val scope = rememberCoroutineScope()

    CollapsedPlayerContent(
        index,
        modifier = Modifier.fillMaxWidth(),
        leadingContent = {
            PlayerArtwork(
                contentDescription = "Song $index",
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.primaryFixed),
            )
        },
        trailingContent = {
            val interactionSource = remember { MutableInteractionSource() }
            IconButton(
                onClick = { scope.launch { sheetState?.show() } },
                interactionSource = interactionSource,
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    painterResource(Res.drawable.ic_keyboard_arrow_down),
                    contentDescription = "Collapse Player"
                )
            }
        },
    )
}

@Composable
fun CollapsedPlayer(
    i: Int,
    showCover: Boolean = false,
) {
    val playerSheet = LocalPlayerSheet.current
    val playerPadding = LocalPlayerPadding.current
    val sheetState = playerSheet?.sheetState
    val scope = rememberCoroutineScope()
    CollapsedPlayerContent(
        i,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.ModulateAlpha
                val sheetProgress = playerSheet?.progressState?.floatValue ?: 0f
                val positiveProgress = sheetProgress.coerceIn(0f, 1f)
                alpha = 1 - positiveProgress
                translationY = -positiveProgress * size.height
            }
            .padding(playerPadding)
            .padding(top = 8.dp)
            .padding(horizontal = collapsedHorizontalPadding.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { scope.launch { sheetState?.expand() } }
            .padding(8.dp),
        leadingContent = {
            Box(Modifier.size(48.dp)) {
                AnimatedVisibility(
                    visible = showCover,
                    enter = fadeIn(tween(LyricsModeTransitionDurationMs)),
                    exit = fadeOut(tween(LyricsModeTransitionDurationMs)),
                ) {
                    PlayerArtwork(
                        contentDescription = "Song $i artwork",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorScheme.primaryFixed),
                    )
                }
            }
        },
        trailingContent = {
            if (!hasTouchInput.value) {
                val interactionSource = remember { MutableInteractionSource() }
                IconButton(
                    onClick = { scope.launch { sheetState?.hide() } },
                    interactionSource = interactionSource,
                    modifier = Modifier.size(40.dp),
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_close), contentDescription = "Close Player"
                    )
                }
            }
        },
    )
}

@Composable
private fun CollapsedPlayerContent(
    i: Int,
    modifier: Modifier = Modifier,
    leadingContent: @Composable () -> Unit,
    trailingContent: @Composable () -> Unit,
) {
    ResponsiveRow(
        modifier = modifier,
        spacing = 8.dp,
    ) {
        item(priority = 90, key = "artwork") {
            leadingContent()
        }

        item(priority = 80, key = "metadata") {
            Column(Modifier.width(128.dp)) {
                val mergedStyle = LocalTextStyle.current.merge(typography.labelLarge)
                Text("Song $i", fontWeight = FontWeight.Bold, style = mergedStyle)
                Text("Artist $i", fontWeight = FontWeight.Normal, style = mergedStyle)
            }
        }

        spacer(
            whenItemHidden = "timeline-volume",
            key = "metadata-favourite-spacer",
            weight = 1f,
        )

        item(priority = 60, key = "favourite") {
            var favourite by remember { mutableStateOf(true) }
            val interactionSource = remember { MutableInteractionSource() }
            FilledTonalIconToggleButton(
                checked = favourite,
                onCheckedChange = { favourite = it },
                modifier = Modifier.size(40.dp),
                shapes = IconButtonDefaults.toggleableShapes(
                    checkedShape = RoundedCornerShape(100)
                ),
                interactionSource = interactionSource,
                colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                    colorScheme.onSecondaryContainer,
                    colorScheme.secondaryContainer,
                    checkedContentColor = colorScheme.tertiaryContainer,
                    checkedContainerColor = colorScheme.onTertiaryContainer
                ),
            ) {
                Icon(
                    painterResource(
                        if (favourite) Res.drawable.ic_favorite_filled
                        else Res.drawable.ic_favorite
                    ),
                    contentDescription = if (favourite) "Favourite" else "Unfavourite"
                )
            }
        }

        item(priority = 10, key = "timeline-volume", weight = 1f) {
            TimelineWithVolume(Modifier.fillMaxWidth())
        }

        item(priority = 49, key = "previous") {
            PreviousButton(Modifier.size(40.dp))
        }

        item(
            priority = 110,
            key = "play-pause-compact",
            whenItemHidden = "timeline-volume",
        ) {
            CompactPlayPauseButton()
        }

        item(
            priority = 110,
            key = "play-pause",
            whenItemVisible = "timeline-volume",
        ) {
            PlayPauseButton(Modifier.size(40.dp))
        }

        item(priority = 50, key = "next") {
            NextButton(Modifier.size(40.dp))
        }

        item(priority = 30, key = "repeat") {
            RepeatButton(Modifier.size(40.dp))
        }

        item(priority = 20, key = "shuffle") {
            ShuffleButton(Modifier.size(40.dp))
        }

        item(priority = 100, key = "trailing") {
            trailingContent()
        }
    }
}
