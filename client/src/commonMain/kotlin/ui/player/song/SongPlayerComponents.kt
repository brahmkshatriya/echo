package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialShapes.Companion.Circle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.playback.PlayableMedia
import dev.brahmkshatriya.echo.app.playback.PlayerTimelineState
import dev.brahmkshatriya.echo.app.ui.components.BetterImage
import dev.brahmkshatriya.echo.app.ui.components.PolygonIconRipple
import dev.brahmkshatriya.echo.app.ui.components.animatedPolygonIconShape
import dev.brahmkshatriya.echo.app.ui.errors.playerTickerAnchor
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerPadding
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSheet
import dev.brahmkshatriya.echo.app.ui.player.artwork.PlayerArtwork
import dev.brahmkshatriya.echo.app.ui.player.collapsedHorizontalPadding
import dev.brahmkshatriya.echo.app.ui.player.components.QueueButton
import dev.brahmkshatriya.echo.app.ui.player.playerBottomBarControlSize
import dev.brahmkshatriya.echo.app.ui.player.playerScrollFade
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.FullLyricsBottomFadeHeight
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.FullLyricsTopFadeHeight
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.Lyrics
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsPanel
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsSelectionItem
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsSelectionPillContent
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsTicker
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LyricsTimingIndex
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.MotionLyrics
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.NoLyricsTicker
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.lyricsEdgeFade
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.rememberMotionLyricsPosition
import echo.client.generated.resources.Res
import echo.client.generated.resources.ic_format_quotes
import echo.client.generated.resources.ic_keyboard_arrow_down
import org.jetbrains.compose.resources.painterResource

private class PlayerHeroContentState(
    val showLyrics: Boolean,
    val showKineticShapes: Boolean,
    val lyrics: Lyrics?,
    val timingIndex: LyricsTimingIndex?,
) {
    override fun equals(other: Any?): Boolean {
        if (
            other !is PlayerHeroContentState ||
                showLyrics != other.showLyrics ||
                showKineticShapes != other.showKineticShapes
        )
            return false
        if (!showLyrics) return true
        return lyrics === other.lyrics && timingIndex === other.timingIndex
    }

    override fun hashCode(): Int =
        if (!showLyrics) {
            if (showKineticShapes) 2 else 0
        } else 31 + if (lyrics == null) 0 else 1
}

private data class PlayerHeroTransitionState(
    val content: PlayerHeroContentState,
    val chromeCollapsed: Boolean,
)

private class PlayerHeroGeometry(val timeline: ExpandedTimelineGeometry) {
    var heightPx by mutableIntStateOf(0)
    var topBarHeightPx by mutableIntStateOf(0)
    val maxCoverHeightPx: Int
        get() = (heightPx - topBarHeightPx - timeline.metadataHeightPx).coerceAtLeast(0)

    fun coverHeightPx(collapseProgress: Float): Float =
        (heightPx - topBarHeightPx - timeline.visibleHeightPx(collapseProgress)).coerceAtLeast(0f)

    fun kineticHeightPx(collapseProgress: Float): Float {
        val progress = collapseProgress.coerceIn(0f, 1f)
        return (heightPx - timeline.visibleHeightPx(progress) * (1f - progress)).coerceAtLeast(0f)
    }
}

@Composable
fun PlayerHero(
    media: PlayableMedia?,
    timelineState: PlayerTimelineState,
    isPlaying: Boolean,
    bassLevel: FloatState,
    onSeek: (Float) -> Unit,
    timelineContent: @Composable (ExpandedTimelineGeometry, Boolean, Boolean, () -> Float) -> Unit,
    lyrics: Lyrics?,
    lyricsTimingIndex: LyricsTimingIndex?,
    showLyrics: Boolean,
    showKineticShapes: Boolean,
    pageActive: Boolean,
    immersiveContentActive: Boolean,
    @Suppress("ModifierParameter") transformModifier: Modifier,
    collapseTimelineRowsForLyrics: Boolean,
    onCollapseTimelineRowsForLyricsChange: (Boolean) -> Unit,
    onLyricsChromeInteraction: () -> Unit,
    onKineticShapesChromeClick: () -> Unit,
    userScrollEnabled: Boolean,
    topPadding: Dp,
    viewportWidth: Dp,
    onLargeArtworkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val immersiveMode = showLyrics || showKineticShapes
    // One clock keeps artwork, immersive content and chrome aligned even
    // when composing the incoming cover takes longer than a frame.
    val heroTransition =
        updateTransition(
            targetState =
                PlayerHeroTransitionState(
                    content =
                        PlayerHeroContentState(
                            showLyrics,
                            showKineticShapes,
                            lyrics,
                            lyricsTimingIndex,
                        ),
                    chromeCollapsed = immersiveMode && collapseTimelineRowsForLyrics,
                ),
            label = "Player hero mode",
        )
    val chromeCollapseProgress =
        heroTransition.animateFloat(
            transitionSpec = {
                tween(LyricsModeTransitionDurationMs, easing = FastOutSlowInEasing)
            },
            label = "Lyrics player chrome collapse",
        ) {
            if (it.chromeCollapsed) 1f else 0f
        }
    val coverVisibility =
        heroTransition.animateFloat(
            transitionSpec = {
                tween(
                    if (!targetState.content.showLyrics && !targetState.content.showKineticShapes)
                        LyricsModeTransitionDurationMs
                    else LyricsModeTransitionDurationMs / 2,
                    easing = FastOutSlowInEasing,
                )
            },
            label = "Player cover visibility",
        ) {
            if (it.content.showLyrics || it.content.showKineticShapes) 0f else 1f
        }
    val lyricsVisibility =
        heroTransition.animateFloat(
            transitionSpec = {
                tween(
                    if (targetState.content.showLyrics) LyricsModeTransitionDurationMs
                    else LyricsModeTransitionDurationMs / 2,
                    easing = FastOutSlowInEasing,
                )
            },
            label = "Full lyrics visibility",
        ) {
            if (it.content.showLyrics) 1f else 0f
        }
    val kineticVisibility =
        heroTransition.animateFloat(
            transitionSpec = {
                tween(LyricsModeTransitionDurationMs, easing = FastOutSlowInEasing)
            },
            label = "Kinetic lyrics visibility",
        ) {
            if (it.content.showKineticShapes) 1f else 0f
        }
    var lyricsMounted by remember { mutableStateOf(false) }
    var kineticMounted by remember { mutableStateOf(false) }
    LaunchedEffect(showLyrics, showKineticShapes, pageActive) {
        if (showLyrics && pageActive) lyricsMounted = true
        if (showKineticShapes && pageActive) kineticMounted = true
    }
    val timelineGeometry = remember { ExpandedTimelineGeometry() }
    val geometry = remember(timelineGeometry) { PlayerHeroGeometry(timelineGeometry) }
    val lyricsViewportHeight =
        remember(geometry, chromeCollapseProgress) {
            { geometry.coverHeightPx(chromeCollapseProgress.value) }
        }
    var compactArtwork by remember { mutableStateOf(false) }

    val currentOnLyricsChromeInteraction by rememberUpdatedState(onLyricsChromeInteraction)
    val currentOnKineticShapesChromeClick by rememberUpdatedState(onKineticShapesChromeClick)
    val kineticShapesInteractionSource = remember { MutableInteractionSource() }
    val chromeInteractionModifier =
        if (immersiveMode)
            Modifier.pointerInput(Unit) {
                awaitEachGesture {
                    var consumedByDescendant: Boolean
                    val down =
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
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
        else Modifier

    Layout(
        modifier = modifier.then(if (immersiveMode) Modifier.clipToBounds() else Modifier),
        content = {
            TopBar(media?.album, transformModifier)
            Box(transformModifier.then(chromeInteractionModifier)) {
                timelineContent(
                    timelineGeometry,
                    immersiveMode && collapseTimelineRowsForLyrics,
                    compactArtwork,
                    { chromeCollapseProgress.value },
                )
            }
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    clip = true
                    shape =
                        PlayerViewportShape(geometry.kineticHeightPx(chromeCollapseProgress.value))
                }
            ) {
                if ((showKineticShapes && pageActive) || kineticMounted)
                    Box(
                        Modifier.fillMaxSize()
                            .retainedPlayerPage(
                                kineticVisibility,
                                compositingStrategy = CompositingStrategy.Offscreen,
                            )
                    ) {
                        Box(
                            modifier =
                                transformModifier
                                    .fillMaxSize()
                                    .clipToBounds()
                                    .lyricsEdgeFade(
                                        topFadeHeight = FullLyricsTopFadeHeight,
                                        bottomFadeHeight = FullLyricsBottomFadeHeight,
                                        viewportHeightPx = {
                                            geometry.kineticHeightPx(chromeCollapseProgress.value)
                                        },
                                        backgroundColor = colorScheme.primaryContainer,
                                    )
                                    .clickable(
                                        interactionSource = kineticShapesInteractionSource,
                                        indication = null,
                                        onClick = { currentOnKineticShapesChromeClick() },
                                    )
                        ) {
                            KineticShapesBackground(
                                bassLevel = bassLevel,
                                artworkKey = media?.artworkUri.orEmpty(),
                                active = showKineticShapes && immersiveContentActive,
                            )
                            val positionMs =
                                rememberMotionLyricsPosition(
                                    timelineState,
                                    active = showKineticShapes && immersiveContentActive,
                                )
                            MotionLyrics(
                                lyrics = lyrics,
                                positionMs = positionMs,
                                modifier =
                                    Modifier.fillMaxSize()
                                        .graphicsLayer {
                                            translationY =
                                                (geometry.kineticHeightPx(
                                                    chromeCollapseProgress.value
                                                ) - geometry.heightPx) / 2f
                                        }
                                        .padding(24.dp),
                            )
                        }
                    }
            }
            Box(
                Modifier.fillMaxSize()
                    .then(
                        if (immersiveMode)
                            Modifier.graphicsLayer {
                                clip = true
                                shape = PlayerViewportShape(lyricsViewportHeight())
                            }
                        else Modifier
                    )
            ) {
                // Keep artwork ready behind the immersive pages so returning
                // to it does not build an image subtree on the first frame.
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = coverVisibility.value }) {
                    CoverArt(
                        album = media?.album,
                        topPadding = topPadding,
                        viewportWidth = viewportWidth,
                        interactive = !immersiveMode,
                        geometry = geometry,
                        chromeCollapseProgress = chromeCollapseProgress,
                        onCompactArtworkChange = { compactArtwork = it },
                        onClick = onLargeArtworkClick,
                    )
                }
                if ((showLyrics && pageActive) || lyricsMounted)
                    Box(Modifier.fillMaxSize().retainedPlayerPage(lyricsVisibility)) {
                        AnimatedContent(
                            targetState =
                                PlayerHeroContentState(
                                    true,
                                    showKineticShapes = false,
                                    lyrics = lyrics,
                                    timingIndex = lyricsTimingIndex,
                                ),
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                            transitionSpec = {
                                val sourceAnimation =
                                    tween<IntOffset>(
                                        LyricsTransitionDurationMs,
                                        easing = FastOutSlowInEasing,
                                    )
                                val enter =
                                    fadeIn(tween(LyricsTransitionDurationMs)) +
                                        slideInVertically(sourceAnimation) { it / 20 }
                                val exit =
                                    fadeOut(tween(LyricsTransitionDurationMs / 2)) +
                                        slideOutVertically(sourceAnimation) { -it / 32 }
                                enter togetherWith exit
                            },
                        ) { state ->
                            if (state.lyrics != null)
                                LyricsPanel(
                                    timelineState = timelineState,
                                    isPlaying = isPlaying,
                                    onSeek = onSeek,
                                    modifier = transformModifier.fillMaxSize(),
                                    lyrics = state.lyrics,
                                    timingIndex = state.timingIndex,
                                    userScrollEnabled = userScrollEnabled,
                                    chromeCollapsed = collapseTimelineRowsForLyrics,
                                    onChromeInteraction = currentOnLyricsChromeInteraction,
                                    onViewportMotionChange = { moving ->
                                        onCollapseTimelineRowsForLyricsChange(moving)
                                    },
                                    active =
                                        showLyrics &&
                                            immersiveContentActive &&
                                            state.lyrics === lyrics,
                                    viewportHeightPx = lyricsViewportHeight,
                                )
                            else
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
                    }
            }
        },
    ) { measurables, constraints ->
        // Measure the largest content viewport once. Chrome movement changes
        // clips/translations rather than constraints on every animation frame.
        val childConstraints = constraints.copy(minHeight = 0)
        val topBar = measurables[0].measure(childConstraints)
        val timeline = measurables[1].measure(childConstraints)
        geometry.heightPx = constraints.maxHeight
        geometry.topBarHeightPx = topBar.height
        val kineticShapes =
            measurables[2].measure(childConstraints.copy(minHeight = constraints.maxHeight))
        val coverHeight = geometry.maxCoverHeightPx
        val cover =
            measurables[3].measure(
                childConstraints.copy(minHeight = coverHeight, maxHeight = coverHeight)
            )

        layout(constraints.maxWidth, constraints.maxHeight) {
            kineticShapes.placeRelative(0, 0)
            topBar.placeRelative(0, 0)
            cover.placeRelative(0, topBar.height)
            timeline.placeRelativeWithLayer(
                0,
                constraints.maxHeight - timelineGeometry.metadataHeightPx,
            ) {
                translationY =
                    -timelineGeometry.secondaryHeightPx *
                        (1f - chromeCollapseProgress.value.coerceIn(0f, 1f))
            }
        }
    }
}

private fun Modifier.retainedPlayerPage(
    visibility: State<Float>,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Auto,
): Modifier = layout { measurable, constraints ->
    val page = measurable.measure(constraints)
    layout(page.width, page.height) {
        if (visibility.value > 0f) {
            page.placeRelativeWithLayer(0, 0) {
                alpha = visibility.value
                // Resolve the kinetic paint and its fade into one surface before
                // ancestor transforms sample their shared edge.
                this.compositingStrategy = compositingStrategy
            }
        }
    }
}

@Composable
private fun CoverArt(
    album: String?,
    topPadding: Dp,
    viewportWidth: Dp,
    interactive: Boolean,
    geometry: PlayerHeroGeometry,
    chromeCollapseProgress: State<Float>,
    onCompactArtworkChange: (Boolean) -> Unit,
    onClick: () -> Unit = {},
) {
    val playerSheet = LocalPlayerSheet.current
    val playerPadding = LocalPlayerPadding.current
    val layoutDirection = LocalLayoutDirection.current
    val animatedTargetX =
        animateDpAsState(
            playerPadding.calculateStartPadding(layoutDirection) +
                (collapsedHorizontalPadding + 8).dp,
            tween(),
        )
    var coverHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val coverInteractive by
        remember(playerSheet) {
            derivedStateOf { (playerSheet?.progressState?.value ?: 1f) > 0.9f }
        }
    val outlineScale =
        remember(playerSheet, density, geometry, chromeCollapseProgress) {
            {
                val progress = (playerSheet?.progressState?.value ?: 1f).coerceIn(0f, 1f)
                val baseSize = coverHeightPx.coerceAtLeast(1)
                val naturalScale =
                    with(density) {
                        (geometry.coverHeightPx(chromeCollapseProgress.value) -
                                (songCoverVerticalPadding * 2).dp.toPx())
                            .coerceIn(0f, baseSize.toFloat()) / baseSize
                    }
                val collapsedScale = with(density) { 48.dp.toPx() / baseSize }
                naturalScale + (collapsedScale - naturalScale) * (1f - progress)
            }
        }
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            PlayerArtwork(
                contentDescription = "${album ?: "Album"} cover art",
                outlineWidth = 0.5.dp,
                outlineRadius = 8.dp,
                outlineScale = outlineScale,
                modifier =
                    Modifier.fillMaxSize()
                        .background(colorScheme.primaryFixed)
                        .clickable(enabled = interactive && coverInteractive, onClick = onClick),
            )
        },
    ) { measurables, constraints ->
        val horizontalPadding = songCoverHorizontalPadding.dp.roundToPx()
        val minimumVerticalPadding = songCoverVerticalPadding.dp.roundToPx()
        val coverSize =
            minOf(
                maxSongCoverSize.dp.roundToPx(),
                (viewportWidth.roundToPx() - horizontalPadding * 2).coerceAtLeast(0),
                (constraints.maxWidth - horizontalPadding * 2).coerceAtLeast(0),
                (constraints.maxHeight - minimumVerticalPadding * 2).coerceAtLeast(0),
            )
        val expandedCoverSize =
            minOf(
                coverSize,
                (geometry.coverHeightPx(0f) - minimumVerticalPadding * 2).toInt().coerceAtLeast(0),
            )
        onCompactArtworkChange(expandedCoverSize < 64.dp.roundToPx())
        coverHeightPx = coverSize
        val artwork = measurables.single().measure(Constraints.fixed(coverSize, coverSize))
        layout(constraints.maxWidth, constraints.maxHeight) {
            artwork.placeRelativeWithLayer(horizontalPadding, 0) {
                val availableHeight = geometry.coverHeightPx(chromeCollapseProgress.value)
                val visibleSize =
                    (availableHeight - minimumVerticalPadding * 2).coerceIn(0f, coverSize.toFloat())
                val verticalPadding =
                    ((availableHeight - visibleSize) / 2f).coerceAtLeast(
                        minimumVerticalPadding.toFloat()
                    )
                val progress = (playerSheet?.progressState?.value ?: 0f).coerceIn(0f, 1f)
                val offset = 1f - progress
                val naturalScale = if (coverSize > 0) visibleSize / coverSize else 1f
                val targetScale = if (coverSize > 0) 48.dp.toPx() / coverSize else 1f
                val coverScale = naturalScale + (targetScale - naturalScale) * offset
                scaleX = coverScale
                scaleY = coverScale
                transformOrigin = TransformOrigin(0f, 0f)
                val center = (viewportWidth.toPx() - visibleSize) / 2f
                translationX =
                    -horizontalPadding + animatedTargetX.value.toPx() * offset + center * progress
                val collapsedY =
                    -(playerSheet?.peekHeight?.toPx() ?: 0f) / 2f -
                        verticalPadding -
                        topPadding.toPx()
                translationY = verticalPadding + collapsedY * offset
                alpha = if (visibleSize < 64.dp.toPx()) 0f else 1f
                clip = true
                shape = RoundedCornerShape(8.dp / coverScale.coerceAtLeast(0.001f))
            }
        }
    }
}

@Composable
fun TopBar(album: String?, modifier: Modifier = Modifier) {
    val playerSheet = LocalPlayerSheet.current
    Row(
        modifier.padding(horizontal = 8.dp).padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(4.dp))
        IconButton(
            onClick = { playerSheet?.collapse() },
            modifier = Modifier.width(32.dp),
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(
                painterResource(Res.drawable.ic_keyboard_arrow_down),
                contentDescription = "Minimize Player",
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            val mergedStyle = LocalTextStyle.current.merge(typography.labelLarge)
            Text(
                "Playing From",
                color = colorScheme.onPrimaryContainer.copy(0.66f),
                fontWeight = FontWeight.Normal,
                style = mergedStyle,
            )
            Text(
                album ?: "Unknown album",
                fontWeight = FontWeight.Bold,
                style = mergedStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {}.padding(8.dp, 4.dp),
            )
        }
        IconButton(
            onClick = {},
            modifier = Modifier.size(44.dp),
            shapes = IconButtonDefaults.shapes(),
        ) {
            BetterImage(
                model = {
                    "https://play-lh.googleusercontent.com/7ynvVIRdhJNAngCg_GI7i8TtH8BqkJYmffeUHsG-mJOdzt1XLvGmbsKuc5Q1SInBjDKN"
                },
                "Spotify",
                modifier = Modifier.padding(4.dp).clip(Circle.toShape()),
            )
        }
    }
}

@Composable
fun BottomBar(
    timelineState: PlayerTimelineState,
    isPlaying: Boolean,
    miniPlayerContent: @Composable () -> Unit,
    index: Int = 0,
    scrollProgress: () -> Float = { 0f },
    lyricsVisible: Boolean = false,
    lyricsChromeCollapsed: Boolean = false,
    selectedLyrics: LyricsSelectionItem? = null,
    lyricsSelectorActive: Boolean = false,
    queueButtonHidden: Boolean = false,
    onLyricsSelectorClick: () -> Unit = {},
    onLyricsPillBoundsChanged: (LayoutCoordinates) -> Unit = {},
    onQueueButtonBoundsChanged: (LayoutCoordinates) -> Unit = {},
    onQueueButtonClick: () -> Unit = {},
    onLyricsClick: () -> Unit = {},
) {
    val backgroundColor = colorScheme.primaryContainer
    Box(
        Modifier.fillMaxWidth()
            .playerTickerAnchor(index)
            .drawBehind {
                drawRect(backgroundColor.copy(alpha = scrollProgress().coerceIn(0f, 1f)))
            }
            .padding(8.dp)
    ) {
        // Each fade spans 60% of the scroll transition, overlapping from 40% to 60%.
        Box(Modifier.playerScrollFade { 1f - scrollProgress() / 0.6f }) {
            LyricsBottomBar(
                timelineState = timelineState,
                isPlaying = isPlaying,
                lyricsVisible = lyricsVisible,
                lyricsChromeCollapsed = lyricsChromeCollapsed,
                selectedLyrics = selectedLyrics,
                lyricsSelectorActive = lyricsSelectorActive,
                queueButtonHidden = queueButtonHidden,
                onLyricsSelectorClick = onLyricsSelectorClick,
                onLyricsPillBoundsChanged = onLyricsPillBoundsChanged,
                onQueueButtonBoundsChanged = onQueueButtonBoundsChanged,
                onQueueButtonClick = onQueueButtonClick,
                onLyricsClick = onLyricsClick,
            )
        }
        Box(Modifier.playerScrollFade { (scrollProgress() - 0.4f) / 0.6f }) { miniPlayerContent() }
    }
}

@Composable
private fun LyricsToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    FilledTonalIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shape =
            animatedPolygonIconShape(
                shape = MaterialShapes.Clover4Leaf,
                pressedShape = MaterialShapes.Square,
                checkedShape = MaterialShapes.Clover4Leaf,
                checked = checked,
                interactionSource = interactionSource,
            ),
        interactionSource = interactionSource,
        colors =
            IconButtonDefaults.filledTonalIconToggleButtonColors(
                colorScheme.secondary,
                colorScheme.onSecondary,
                checkedContainerColor = colorScheme.tertiary,
                checkedContentColor = colorScheme.onTertiary,
            ),
    ) {
        PolygonIconRipple(interactionSource) {
            Crossfade(targetState = checked, animationSpec = motionScheme.fastSpatialSpec()) {
                isChecked ->
                Icon(
                    painterResource(Res.drawable.ic_format_quotes),
                    contentDescription = if (isChecked) "Hide lyrics" else "Show lyrics",
                )
            }
        }
    }
}

@Composable
private fun LyricsBottomBar(
    timelineState: PlayerTimelineState,
    isPlaying: Boolean,
    lyricsVisible: Boolean,
    lyricsChromeCollapsed: Boolean,
    selectedLyrics: LyricsSelectionItem?,
    lyricsSelectorActive: Boolean,
    queueButtonHidden: Boolean,
    onLyricsSelectorClick: () -> Unit,
    onLyricsPillBoundsChanged: (LayoutCoordinates) -> Unit,
    onQueueButtonBoundsChanged: (LayoutCoordinates) -> Unit,
    onQueueButtonClick: () -> Unit,
    onLyricsClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(playerBottomBarControlSize),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LyricsToggle(checked = lyricsVisible, onCheckedChange = { onLyricsClick() })
        Box(
            Modifier.height(playerBottomBarControlSize)
                .weight(1f)
                .onGloballyPositioned { onLyricsPillBoundsChanged(it) }
                .graphicsLayer { alpha = if (lyricsSelectorActive) 0f else 1f }
                .clip(RoundedCornerShape(playerBottomBarControlSize / 2f))
                .background(
                    color = colorScheme.onPrimaryContainer.copy(0.1f),
                    shape = RoundedCornerShape(playerBottomBarControlSize / 2f),
                )
                .clickable(
                    enabled = !lyricsSelectorActive,
                    onClick = if (lyricsVisible) onLyricsSelectorClick else onLyricsClick,
                )
        ) {
            val selectionAlpha =
                animateFloatAsState(
                    targetValue = if (lyricsVisible) 1f else 0f,
                    animationSpec = tween(LyricsTransitionDurationMs),
                    label = "Lyrics source pill visibility",
                )
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - selectionAlpha.value }) {
                if (selectedLyrics != null)
                    LyricsTicker(
                        lyrics = selectedLyrics.lyrics,
                        timelineState = timelineState,
                        isPlaying = isPlaying,
                        active = !lyricsVisible,
                    )
                else NoLyricsTicker()
            }
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = selectionAlpha.value }) {
                Crossfade(
                    targetState = selectedLyrics,
                    animationSpec = tween(LyricsTransitionDurationMs),
                ) { selection ->
                    LyricsSelectionPillContent(
                        selectedLyrics = selection,
                        chromeCollapsed = lyricsChromeCollapsed && lyricsVisible,
                    )
                }
            }
        }

        QueueButton(
            onClick = onQueueButtonClick,
            enabled = !queueButtonHidden,
            modifier =
                Modifier.onGloballyPositioned(onQueueButtonBoundsChanged).graphicsLayer {
                    alpha = if (queueButtonHidden) 0f else 1f
                },
        )
    }
}
