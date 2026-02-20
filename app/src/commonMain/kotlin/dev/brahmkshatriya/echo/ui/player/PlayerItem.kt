package dev.brahmkshatriya.echo.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialShapes.Companion.Circle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.kmpalette.color
import com.kmpalette.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.rememberDynamicMaterialThemeState
import com.skydoves.landscapist.palette.PalettePlugin
import com.skydoves.landscapist.palette.rememberPaletteState
import dev.brahmkshatriya.echo.platform.onPointerScrollY
import dev.brahmkshatriya.echo.ui.Media
import dev.brahmkshatriya.echo.ui.components.BetterImage
import dev.brahmkshatriya.echo.ui.components.FastScrollbar
import dev.brahmkshatriya.echo.ui.components.LocalMainBackStack
import dev.brahmkshatriya.echo.ui.components.materialGroup
import dev.brahmkshatriya.echo.ui.components.rememberBasicScrollbarThumbMover
import dev.brahmkshatriya.echo.ui.components.scrollbarState
import dev.brahmkshatriya.echo.ui.components.simpleTween
import dev.brahmkshatriya.echo.ui.theme.Primary
import echo.app.generated.resources.Res
import echo.app.generated.resources.ic_close
import echo.app.generated.resources.ic_favorite
import echo.app.generated.resources.ic_favorite_filled
import echo.app.generated.resources.ic_keyboard_arrow_down
import echo.app.generated.resources.ic_lyrics
import echo.app.generated.resources.ic_lyrics_filled
import echo.app.generated.resources.ic_play_arrow
import echo.app.generated.resources.ic_repeat
import echo.app.generated.resources.ic_shuffle
import echo.app.generated.resources.ic_skip_next
import echo.app.generated.resources.ic_skip_previous
import echo.app.generated.resources.ic_volume_up
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import ir.mahozad.multiplatform.wavyslider.material3.Track
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun PlayerItem(i: Int) {
    val paletteState = rememberPaletteState()
    val color = paletteState.value?.let {
        (it.vibrantSwatch ?: it.dominantSwatch ?: it.lightVibrantSwatch)?.color
    } ?: Primary
    val scheme = rememberDynamicMaterialThemeState(
        isDark = isSystemInDarkTheme(),
        style = PaletteStyle.Rainbow,
        specVersion = ColorSpec.SpecVersion.SPEC_2021,
        seedColor = color,
        neutral = color,
        neutralVariant = color,
    ).colorScheme
    MaterialExpressiveTheme(animateColorScheme(scheme)) {
        Box(Modifier.playerBackground(true)) {
            SongPlayerItem(i) { paletteState.value = it }
        }
    }
}

val maxSongCoverSize = 360.dp
val maxSongCoverHeight = 400.dp
val songCoverHorizontalPadding = 16.dp
val songCoverVerticalPadding = 8.dp

fun Modifier.coverSize() = padding(songCoverHorizontalPadding, songCoverVerticalPadding)
    .widthIn(max = maxSongCoverSize)
    .height(maxSongCoverHeight)
    .aspectRatio(1f)
    .fillMaxSize()

@Composable
fun BoxScope.SongPlayerItem(
    i: Int,
    paletteState: (Palette) -> Unit,
) = CompositionLocalProvider(
    LocalContentColor provides colorScheme.onPrimaryContainer
) {
    CollapsedPlayer(i)
    val widthState = remember { mutableIntStateOf(0) }
    val heightState = remember { mutableIntStateOf(0) }
    val topBarHeight = remember { mutableIntStateOf(0) }

    val playerSheet = LocalPlayerSheet.current
    val scope = rememberCoroutineScope()
    val backStack = LocalMainBackStack.current
    val cardColors = CardDefaults.cardColors(
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface
    )
    val listState = rememberLazyListState()
    LazyColumn(
        Modifier.onSizeChanged {
            widthState.intValue = it.width
            heightState.intValue = it.height
        }.graphicsLayer {
            val sheetProgress = playerSheet?.progressState?.floatValue ?: 0f
            val positiveProgress = sheetProgress.coerceIn(0f, 1f)
            val offset = 1 - sheetProgress.coerceIn(0f, 1f)
            alpha = if (positiveProgress > 0.75f) (positiveProgress - 0.75f) * 4 else 0f
            translationY = offset * size.height
        },
        contentPadding = PaddingValues(
            bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
        ),
        state = listState
    ) {
        item { TopBar(i) { topBarHeight.intValue = it } }
        item { Box(Modifier.coverSize()) }
        item { SongController(i) }
        materialGroup {
            (0..10).forEach {
                card(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    key = "$i$it",
                    contentType = i,
                    colors = cardColors
                ) {
                    Box(
                        Modifier.fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    playerSheet?.sheetState?.partialExpand()
                                    backStack?.add(Media(it.toString()))
                                }
                            }.padding(16.dp, 24.dp)
                    ) { Text("Item $it") }
                }
            }
        }
    }

    CoverArt(
        i,
        paletteState,
        { topBarHeight.intValue },
        { widthState.intValue },
        {
            when (listState.firstVisibleItemIndex) {
                0 -> listState.firstVisibleItemScrollOffset
                1 -> listState.firstVisibleItemScrollOffset + topBarHeight.intValue
                else -> heightState.intValue
            }
        }
    )
    val scrollbarState = listState.scrollbarState(12)
    FastScrollbar(
        modifier = Modifier
            .fillMaxHeight()
            .width(12.dp)
            .padding(end = 4.dp, top = 4.dp, bottom = 4.dp)
            .safeDrawingPadding()
            .graphicsLayer {
                alpha = playerSheet?.progressState?.floatValue ?: 1f
            }
            .align(Alignment.TopEnd),
        state = scrollbarState,
        scrollInProgress = listState.isScrollInProgress,
        orientation = Orientation.Vertical,
        onThumbMoved = listState.rememberBasicScrollbarThumbMover()
    )
}

@Composable
fun SongController(i: Int) = Box {
    val maxRange = 6_000f
    val animatedValue = remember { Animatable(0f) }
    var showRemainingTime by remember { mutableStateOf(false) }
    val currentTimeLabel by remember { derivedStateOf { formatTime(animatedValue.value) } }
    val endLabel by remember(showRemainingTime) {
        derivedStateOf {
            if (showRemainingTime) {
                "-${formatTime(maxRange - animatedValue.value)}"
            } else {
                formatTime(maxRange)
            }
        }
    }
    val heightState = remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val mergedStyle = LocalTextStyle.current.merge(typography.labelLarge)

    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        PlayerSlider(
            Modifier.weight(1f).padding(top = heightState.value),
            Modifier.height(72.dp).padding(vertical = 20.dp),
            maxRange
        ) { animatedValue }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.padding(horizontal = 16.dp).onSizeChanged {
                heightState.value = density.run { it.height.toDp() } - 8.dp
            },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("Song $i")
                Text("Artist $i", color = colorScheme.primary)
            }
            LyricsToggle()
            LikeToggle()
        }
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
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
                modifier = Modifier.clickable(interactionSource = endInteraction) {
                    showRemainingTime = !showRemainingTime
                }.padding(8.dp)
            )
        }
    }
    Text(
        text = "FLAC",
        style = mergedStyle,
        modifier = Modifier.padding(8.dp).align(Alignment.BottomCenter),
    )
}

@Composable
fun LyricsToggle() {
    var lyrics by remember { mutableStateOf(true) }
    val interactionSource = remember { MutableInteractionSource() }
    FilledTonalIconToggleButton(
        checked = lyrics,
        onCheckedChange = { lyrics = it },
        modifier = Modifier.size(44.dp),
        shapes = IconButtonDefaults.toggleableShapes(
            checkedShape = RoundedCornerShape(100)
        ),
        interactionSource = interactionSource,
        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
            colorScheme.primary.copy(0.25f),
            colorScheme.onPrimaryContainer,
            checkedContentColor = colorScheme.onPrimary,
            checkedContainerColor = colorScheme.primary
        ),
    ) {
        Icon(
            painterResource(
                if (lyrics) Res.drawable.ic_lyrics_filled
                else Res.drawable.ic_lyrics
            ),
            contentDescription = if (lyrics) "Lyrics On" else "Lyrics Off"
        )
    }
}


@Composable
fun LikeToggle() {
    var favourite by remember { mutableStateOf(true) }
    val interactionSource = remember { MutableInteractionSource() }
    FilledTonalIconToggleButton(
        checked = favourite,
        onCheckedChange = { favourite = it },
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
                if (favourite) Res.drawable.ic_favorite_filled
                else Res.drawable.ic_favorite
            ),
            contentDescription = if (favourite) "Favourite" else "Unfavourite"
        )
    }
}

@Composable
fun Modifier.playerBackground(colored: Boolean = false): Modifier {
    val playerSheet = LocalPlayerSheet.current
    val playerPadding = LocalPlayerPadding.current
    val peekHeight = playerSheet?.peekHeight ?: 72.dp
    val layoutDirection = LocalLayoutDirection.current
    val startPadding = playerPadding.calculateStartPadding(layoutDirection)
    val endPadding = playerPadding.calculateEndPadding(layoutDirection)
    val animatedStart = animateDpAsState(startPadding + 12.dp, simpleTween())
    val animatedEnd = animateDpAsState(endPadding + 12.dp, simpleTween())

    return fillMaxSize().graphicsLayer {
        val sheetProgress = playerSheet?.progressState?.floatValue ?: 0f
        val positiveProgress = sheetProgress.coerceIn(0f, 1f)
        val backProgress = playerSheet?.backProgressState?.floatValue ?: 0f
        clip = true
        shape = ClippedShape(
            peekHeight - 8.dp,
            positiveProgress,
            backProgress,
            animatedStart.value,
            animatedEnd.value
        )
    }.run {
        if (colored) background(colorScheme.primaryContainer) else this
    }
}

@Composable
fun CoverArt(
    i: Int,
    paletteState: (Palette) -> Unit,
    topBarHeight: () -> Int,
    widthState: () -> Int,
    scrollOffset: () -> Int
) {
    val playerSheet = LocalPlayerSheet.current
    val playerPadding = LocalPlayerPadding.current

    val layoutDirection = LocalLayoutDirection.current
    val artWorks = LocalPlayerItems.current
    val image = artWorks[i]

    val animatedTargetX = animateDpAsState(
        playerPadding.calculateStartPadding(layoutDirection) + 20.dp,
        simpleTween()
    )
    BetterImage(
        { image },
        "Song $i",
        Modifier
            .coverSize()
            .graphicsLayer {
                val sheetProgress = playerSheet?.progressState?.floatValue ?: 0f
                val positiveProgress = sheetProgress.coerceIn(0f, 1f)
                val offset = 1 - positiveProgress

                val targetX = animatedTargetX.value.toPx()
                val targetY = 8.dp.toPx() + (size.height - maxSongCoverHeight.toPx()) / 2f
                val targetSize = 48.dp
                val targetScale = targetSize.toPx() / size.height
                scaleX = 1 + (targetScale - 1) * offset
                scaleY = scaleX
                transformOrigin = TransformOrigin(0f, 0f)
                val center = (widthState() - size.width) / 2f
                translationX =
                    -songCoverHorizontalPadding.toPx() + targetX * offset + center * positiveProgress

                translationY = (-songCoverVerticalPadding.toPx() + targetY) * offset +
                        (topBarHeight() - scrollOffset()) * positiveProgress

                clip = true
                shape = RoundedCornerShape((8 / scaleX).dp)
            }
            .background(colorScheme.primaryFixed),
        PalettePlugin { paletteState(it) }
    )
}

@Composable
fun TopBar(index: Int, onHeightChanged: (Int) -> Unit) {
    val playerSheet = LocalPlayerSheet.current
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val sheetState = playerSheet?.sheetState
    val scope = rememberCoroutineScope()
    Row(
        Modifier.padding(end = 8.dp)
            .onSizeChanged {
                onHeightChanged(it.height)
            }
            .padding(top = safePadding.calculateTopPadding())
    ) {
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
fun CollapsedPlayer(i: Int) {
    val playerSheet = LocalPlayerSheet.current
    val playerPadding = LocalPlayerPadding.current
    val sheetState = playerSheet?.sheetState
    val scope = rememberCoroutineScope()

    val maxWidth = remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val sheetProgress = playerSheet?.progressState?.floatValue ?: 0f
                val positiveProgress = sheetProgress.coerceIn(0f, 1f)
                alpha = 1 - positiveProgress
                translationY = -positiveProgress * size.height
            }
            .padding(playerPadding)
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { scope.launch { sheetState?.expand() } }
            .padding(8.dp)
            .onSizeChanged { maxWidth.value = density.run { it.width.toDp() } },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        @Composable
        fun item(min: Dp, max: Dp = maxWidth.value, block: @Composable () -> Unit) {
            if (maxWidth.value < min) return
            if (maxWidth.value > max) return
            block()
        }

        item(120.dp) {
            Spacer(Modifier.padding(start = 4.dp).size(48.dp))
        }

        item(256.dp) {
            Column(Modifier.width(128.dp)) {
                val mergedStyle = LocalTextStyle.current.merge(typography.labelLarge)
                Text("Song $i", fontWeight = FontWeight.Bold, style = mergedStyle)
                Text("Artist $i", fontWeight = FontWeight.Normal, style = mergedStyle)
            }
        }

        val timelineShow = 780.dp
        item(0.dp, timelineShow - 1.dp) { Spacer(Modifier.weight(1f)) }

        item(360.dp) {
            var favourite by remember { mutableStateOf(true) }
            val interactionSource = remember { MutableInteractionSource() }
            FilledTonalIconToggleButton(
                checked = favourite,
                onCheckedChange = { favourite = it },
                modifier = Modifier.size(44.dp),
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

        item(timelineShow) { Timeline() }
        item(600.dp) { VolumeAdjuster() }

        val playButtonShow = 480.dp
        item(310.dp, playButtonShow - 1.dp) {
            val interactionSource = remember { MutableInteractionSource() }
            FilledIconButton(
                onClick = { },
                interactionSource = interactionSource,
                modifier = Modifier.size(48.dp),
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    painterResource(Res.drawable.ic_play_arrow), contentDescription = "Play"
                )
            }
        }

        item(670.dp) {
            val interactionSource = remember { MutableInteractionSource() }
            IconButton(
                onClick = { scope.launch { sheetState?.hide() } },
                interactionSource = interactionSource,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    colorScheme.onSecondaryContainer,
                    colorScheme.secondaryContainer
                ),
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    painterResource(Res.drawable.ic_repeat), contentDescription = "Close Player"
                )
            }
        }

        item(playButtonShow) {
            Box(Modifier.padding(horizontal = 0.dp)) {
                Box(
                    Modifier
                        .width(136.dp)
                        .height(48.dp)
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(100))
                        .background(colorScheme.primary.copy(0.25f))
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    IconButton(
                        onClick = { scope.launch { sheetState?.hide() } },
                        modifier = Modifier.size(40.dp),
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_skip_previous),
                            contentDescription = "Close Player"
                        )
                    }
                    FilledIconButton(
                        onClick = { },
                        modifier = Modifier.size(48.dp),
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_play_arrow), contentDescription = "Play"
                        )
                    }
                    IconButton(
                        onClick = { scope.launch { sheetState?.hide() } },
                        modifier = Modifier.size(40.dp),
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_skip_next),
                            contentDescription = "Close Player"
                        )
                    }
                }
            }
        }

        item(710.dp) {
            val interactionSource = remember { MutableInteractionSource() }
            IconButton(
                onClick = { scope.launch { sheetState?.hide() } },
                interactionSource = interactionSource,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    colorScheme.onSecondaryContainer,
                    colorScheme.secondaryContainer
                ),
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    painterResource(Res.drawable.ic_shuffle), contentDescription = "Close Player"
                )
            }
        }

        item(0.dp) {
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
    }
}

fun formatTime(ms: Float): String {
    val totalSeconds = (ms / 1000).toInt().coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun RowScope.Timeline() {
    val mergedStyle = LocalTextStyle.current.merge(typography.labelLarge)

    val maxRange = 6_000f
    val animatedValue = remember { Animatable(0f) }
    var showRemainingTime by remember { mutableStateOf(false) }
    val currentTimeLabel by remember { derivedStateOf { formatTime(animatedValue.value) } }

    val endLabel by remember(showRemainingTime) {
        derivedStateOf {
            if (showRemainingTime) {
                "-${formatTime(maxRange - animatedValue.value)}"
            } else {
                formatTime(maxRange)
            }
        }
    }
    Text(
        text = currentTimeLabel,
        style = mergedStyle,
        modifier = Modifier.widthIn(min = 40.dp),
        textAlign = TextAlign.Center
    )

    PlayerSlider(Modifier.weight(1f), maxRange = maxRange) { animatedValue }

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

@Composable
fun PlayerSlider(
    modifier: Modifier = Modifier,
    thumbModifier: Modifier = Modifier,
    maxRange: Float,
    animatedValue: () -> Animatable<Float, AnimationVector1D>
) {
    val rangeMS = remember { 0f..maxRange }
    val animatedValue = animatedValue()
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isDragged, animatedValue.targetValue) {
        if (!isDragged) {
            while (animatedValue.value < maxRange) {
                val nextTarget = (animatedValue.value + 1000f).coerceAtMost(maxRange)
                animatedValue.animateTo(
                    targetValue = nextTarget,
                    animationSpec = tween(1000, easing = LinearEasing)
                )
            }
        }
    }
    val scope = rememberCoroutineScope()
    Slider(
        valueRange = rangeMS,
        value = animatedValue.value,
        onValueChange = { newValue ->
            scope.launch {
                animatedValue.snapTo(newValue)
            }
        },
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        interactionSource = interactionSource,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                modifier = thumbModifier,
                thumbSize = DpSize(4.dp, 32.dp)
            )
        },
        track = {
            SliderDefaults.Track(
                it,
                colors = SliderDefaults.colors(
                    activeTrackColor = colorScheme.primary,
                    inactiveTrackColor = colorScheme.primary.copy(0.25f)
                ),
                waveLength = 24.dp,
                waveHeight = 8.dp,
                waveVelocity = 8.dp to WaveDirection.TAIL,
                waveThickness = 4.dp,
                trackThickness = 4.dp,
            )
        }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VolumeAdjuster() {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState()
    val position = remember { mutableFloatStateOf(1f) }
    val sliderInteraction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(100))
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource) {

            }
            .onPointerScrollY { delta ->
                val newVolume = position.floatValue - delta * 0.05f
                position.floatValue = newVolume.coerceIn(0f, 1f)
            }
            .background(colorScheme.primary.copy(0.25f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(isHovered.value) {
            Slider(
                value = position.floatValue,
                modifier = Modifier.width(128.dp).padding(horizontal = 8.dp),
                interactionSource = sliderInteraction,
                onValueChange = { position.floatValue = it },
                thumb = {
                    SliderDefaults.Thumb(
                        sliderInteraction,
                        thumbSize = DpSize(4.dp, 28.dp)
                    )
                },
                track = {
                    SliderDefaults.Track(
                        it,
                        Modifier.height(4.dp),
                        colors = SliderDefaults.colors(
                            activeTrackColor = colorScheme.primary,
                            inactiveTrackColor = colorScheme.primary.copy(0.25f)
                        )
                    )
                }
            )
        }
        Icon(
            painterResource(Res.drawable.ic_volume_up), contentDescription = "Close Player"
        )
    }
}