package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialShapes.Companion.Circle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.onPointerScrollY
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kmpalette.color
import com.kmpalette.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.rememberDynamicMaterialThemeState
import com.skydoves.landscapist.palette.PalettePlugin
import com.skydoves.landscapist.palette.rememberPaletteState
import dev.brahmkshatriya.echo.app.ui.Media
import dev.brahmkshatriya.echo.app.ui.components.BetterImage
import dev.brahmkshatriya.echo.app.ui.components.BetterSheet
import dev.brahmkshatriya.echo.app.ui.components.FastScrollbar
import dev.brahmkshatriya.echo.app.ui.components.LocalMainBackStack
import dev.brahmkshatriya.echo.app.ui.components.ScrollbarState
import dev.brahmkshatriya.echo.app.ui.components.materialGroup
import dev.brahmkshatriya.echo.app.ui.components.paddingMask
import dev.brahmkshatriya.echo.app.ui.components.scrollbarStateValue
import dev.brahmkshatriya.echo.app.ui.main.Header
import dev.brahmkshatriya.echo.app.ui.theme.Primary
import echo.app.generated.resources.Res
import echo.app.generated.resources.ic_close
import echo.app.generated.resources.ic_favorite
import echo.app.generated.resources.ic_favorite_filled
import echo.app.generated.resources.ic_keyboard_arrow_down
import echo.app.generated.resources.ic_mic_music_3
import echo.app.generated.resources.ic_more_vert
import echo.app.generated.resources.ic_pause
import echo.app.generated.resources.ic_pause_32
import echo.app.generated.resources.ic_play_arrow
import echo.app.generated.resources.ic_play_arrow_32
import echo.app.generated.resources.ic_queue_music
import echo.app.generated.resources.ic_repeat
import echo.app.generated.resources.ic_shuffle
import echo.app.generated.resources.ic_skip_next
import echo.app.generated.resources.ic_skip_next_32
import echo.app.generated.resources.ic_skip_previous
import echo.app.generated.resources.ic_skip_previous_32
import echo.app.generated.resources.ic_volume_up
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun PlayerItem(
    i: Int,
    onScrolledToTopChanged: (Boolean) -> Unit = {},
) {
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
            SongPlayerItem(
                i = i,
                onScrolledToTopChanged = onScrolledToTopChanged,
            ) { paletteState.value = it }
        }
    }
}

const val maxSongCoverSize = 360
const val songCoverHorizontalPadding = 16
const val songCoverVerticalPadding = 16
const val collapsedHorizontalPadding = 8
private val coverArtViewportReserve = 296.dp
private const val PlayerBottomBarContentType = "player-bottom-bar"
private const val PlayerQueueItemContentType = "player-queue-item"
private const val PlayerQueueItemsPerGroup = 11
private const val PlayerScrollbarThumbSizePercent = 0.16f
private const val LyricsWaitingGapMs = 1_000L
private const val LyricsTransitionDurationMs = 240
private const val LyricsWordFadeLeadMs = 300L
private const val LyricsModeTransitionDurationMs = 320

@Stable
private class PlayerTimelineState(val durationMs: Float) {
    var positionMs by mutableFloatStateOf(0f)
    var isSeeking by mutableStateOf(false)
}

private val LocalPlayerTimelineState = staticCompositionLocalOf<PlayerTimelineState?> { null }

private suspend fun PagerState.playPrevious() {
    if (pageCount <= 0) return
    animateScrollToPage((currentPage - 1 + pageCount) % pageCount)
}

private suspend fun PagerState.playNext(shuffle: Boolean) {
    if (pageCount <= 0) return
    val targetPage = if (shuffle && pageCount > 1)
        (currentPage + Random.nextInt(1, pageCount)) % pageCount
    else (currentPage + 1) % pageCount
    animateScrollToPage(targetPage)
}

@Stable
private class PlayerScrollbarItemSizes {
    private val sizes = mutableMapOf<Int, Int>()

    fun update(index: Int, size: Int) {
        if (size > 0) sizes[index] = size
    }

    fun itemSize(index: Int, fallbackSize: Int): Int = sizes[index] ?: fallbackSize

    fun fallbackSize(viewportSize: Int): Int = sizes.values
        .filter { it > 0 }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.roundToInt()
        ?: viewportSize.coerceAtLeast(1)
}

private fun estimatedPlayerContentHeight(
    itemSizes: PlayerScrollbarItemSizes,
    totalItems: Int,
    fallbackSize: Int,
): Int = (0 until totalItems).sumOf { index ->
    itemSizes.itemSize(index, fallbackSize)
}

private fun estimatedPlayerScrollOffset(
    itemSizes: PlayerScrollbarItemSizes,
    firstVisibleIndex: Int,
    firstVisibleItemScrollOffset: Int,
    fallbackSize: Int,
): Int {
    val beforeFirstItem = (0 until firstVisibleIndex).sumOf { index ->
        itemSizes.itemSize(index, fallbackSize)
    }
    val firstItemSize = itemSizes.itemSize(firstVisibleIndex, fallbackSize)
    return beforeFirstItem + firstVisibleItemScrollOffset.coerceIn(0, firstItemSize)
}

@Composable
private fun rememberPlayerScrollbarState(
    listState: LazyListState,
    itemSizes: PlayerScrollbarItemSizes,
): ScrollbarState {
    val state = remember { ScrollbarState() }
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            PlayerScrollbarSnapshot(
                firstVisibleIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                canScrollBackward = listState.canScrollBackward,
                totalItems = layoutInfo.totalItemsCount,
                viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset,
                visibleItemSizes = layoutInfo.visibleItemsInfo.map { it.index to it.size },
            )
        }.collect { snapshot ->
            if (snapshot.totalItems <= 0) return@collect
            snapshot.visibleItemSizes.forEach { (index, size) ->
                itemSizes.update(index, size)
            }

            val viewportSize = snapshot.viewportSize.coerceAtLeast(1)
            val fallbackSize = itemSizes.fallbackSize(viewportSize)
            val totalHeight = estimatedPlayerContentHeight(
                itemSizes = itemSizes,
                totalItems = snapshot.totalItems,
                fallbackSize = fallbackSize,
            )
            val maxScrollOffset = (totalHeight - viewportSize).coerceAtLeast(1)
            val scrollOffset = estimatedPlayerScrollOffset(
                itemSizes = itemSizes,
                firstVisibleIndex = snapshot.firstVisibleIndex,
                firstVisibleItemScrollOffset = snapshot.firstVisibleItemScrollOffset,
                fallbackSize = fallbackSize,
            )
            val maxThumbTravel = 1f - PlayerScrollbarThumbSizePercent
            val thumbMovedPercent = when {
                !snapshot.canScrollBackward -> 0f
                else -> (scrollOffset.toFloat() / maxScrollOffset * maxThumbTravel)
                    .coerceIn(0f, maxThumbTravel)
            }
            state.onScroll(
                scrollbarStateValue(
                    thumbSizePercent = PlayerScrollbarThumbSizePercent,
                    thumbMovedPercent = thumbMovedPercent,
                )
            )
        }
    }
    return state
}

private data class PlayerScrollbarSnapshot(
    val firstVisibleIndex: Int,
    val firstVisibleItemScrollOffset: Int,
    val canScrollBackward: Boolean,
    val totalItems: Int,
    val viewportSize: Int,
    val visibleItemSizes: List<Pair<Int, Int>>,
)

@Composable
private fun rememberPlayerScrollbarThumbMover(
    listState: LazyListState,
    itemSizes: PlayerScrollbarItemSizes,
): (Float) -> Unit {
    var thumbMovedPercent by remember { mutableFloatStateOf(Float.NaN) }
    LaunchedEffect(thumbMovedPercent) {
        if (thumbMovedPercent.isNaN()) return@LaunchedEffect
        val layoutInfo = listState.layoutInfo
        val totalItems = layoutInfo.totalItemsCount
        if (totalItems <= 0) return@LaunchedEffect

        val viewportSize = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset)
            .coerceAtLeast(1)
        val fallbackSize = itemSizes.fallbackSize(viewportSize)
        val totalHeight = estimatedPlayerContentHeight(
            itemSizes = itemSizes,
            totalItems = totalItems,
            fallbackSize = fallbackSize,
        )
        val maxScrollOffset = (totalHeight - viewportSize).coerceAtLeast(0)
        val maxThumbTravel = 1f - PlayerScrollbarThumbSizePercent
        val targetScrollOffset = (maxScrollOffset *
                (thumbMovedPercent / maxThumbTravel).coerceIn(0f, 1f))
            .roundToInt()

        var remainingOffset = targetScrollOffset
        var targetIndex = 0
        while (targetIndex < totalItems - 1) {
            val itemSize = itemSizes.itemSize(targetIndex, fallbackSize)
            if (remainingOffset < itemSize) break
            remainingOffset -= itemSize
            targetIndex++
        }
        listState.requestScrollToItem(targetIndex, remainingOffset)
    }
    return remember {
        { newPercentage -> thumbMovedPercent = newPercentage }
    }
}

fun Modifier.coverSize(
    maxCoverSize: Dp,
    verticalPadding: Dp = songCoverVerticalPadding.dp,
) = padding(songCoverHorizontalPadding.dp, verticalPadding)
    .widthIn(max = maxCoverSize)
    .height(maxCoverSize)
    .aspectRatio(1f)
    .fillMaxSize()

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongPlayerItem(
    i: Int,
    onScrolledToTopChanged: (Boolean) -> Unit = {},
    paletteState: (Palette) -> Unit,
) = CompositionLocalProvider(
    LocalContentColor provides colorScheme.onPrimaryContainer
) {
    val timelineState = remember(i) {
        PlayerTimelineState(niceLyrics.lines.lastOrNull()?.endMs?.toFloat() ?: 0f)
    }
    val controls = LocalPlayerControls.current
    val pagerState = LocalPlayerPagerState.current
    LaunchedEffect(timelineState, controls, pagerState) {
        var previousFrameNanos = withFrameNanos { it }
        while (isActive) {
            val frameNanos = withFrameNanos { it }
            val isCurrentItem = pagerState == null || pagerState.currentPage == i
            val shouldAdvance = isCurrentItem && controls?.isPlaying != false &&
                    !timelineState.isSeeking &&
                    timelineState.positionMs < timelineState.durationMs
            if (shouldAdvance) {
                val elapsedMs = (frameNanos - previousFrameNanos) / 1_000_000f
                timelineState.positionMs =
                    (timelineState.positionMs + elapsedMs).coerceAtMost(timelineState.durationMs)
                if (timelineState.positionMs >= timelineState.durationMs) {
                    timelineState.positionMs = 0f
                    if (controls?.repeatEnabled != true) {
                        pagerState?.playNext(controls?.shuffleEnabled == true)
                    }
                }
            }
            previousFrameNanos = frameNanos
        }
    }
    CompositionLocalProvider(LocalPlayerTimelineState provides timelineState) {
        BoxWithConstraints {
            val density = LocalDensity.current
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
            var showLyrics by remember(i) { mutableStateOf(false) }
            val layoutDirection = LocalLayoutDirection.current
            val safeDrawing = WindowInsets.safeDrawing.asPaddingValues()
            val topPadding = safeDrawing.calculateTopPadding()
            val bottomPadding = safeDrawing.calculateBottomPadding()
            val timelineHeight = remember { mutableIntStateOf(0) }
            val controllerHeight = remember { mutableIntStateOf(0) }
            val bottomBarHeight = remember { mutableIntStateOf(0) }
            val currentOnScrolledToTopChanged by rememberUpdatedState(onScrolledToTopChanged)

            LaunchedEffect(listState) {
                snapshotFlow { !listState.canScrollBackward }
                    .collect { currentOnScrolledToTopChanged(it) }
            }
            playerSheet?.let { sheet ->
                LaunchedEffect(sheet, listState) {
                    snapshotFlow { sheet.progressState.floatValue }.collect { progress ->
                        if (progress < 0.75f && listState.canScrollBackward) {
                            withFrameNanos { }
                            listState.scrollToItem(0)
                        }
                    }
                }
            }

            val bottomBarKey = remember(i) { "player-bottom-bar-$i" }
            val isBottomBarSticky by remember(bottomBarKey) {
                derivedStateOf {
                    val layoutInfo = listState.layoutInfo
                    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.key == bottomBarKey }
                    item != null && item.offset <= 0 && listState.firstVisibleItemIndex >= item.index
                }
            }
            val transformModifier =
                Modifier.expandedListItemTransform(playerSheet) { heightState.intValue }
            val coverSlotHeight = remember(
                maxHeight,
                heightState.intValue,
                topBarHeight.intValue,
                timelineHeight.intValue,
                controllerHeight.intValue,
                bottomBarHeight.intValue,
                topPadding,
                bottomPadding
            ) {
                val measuredHeight = with(density) { heightState.intValue.toDp() }
                val viewportHeight = if (measuredHeight > 0.dp) measuredHeight else maxHeight
                val topBarReserve = with(density) { topBarHeight.intValue.toDp() }
                val measuredBottomReserve = with(density) {
                    (timelineHeight.intValue + controllerHeight.intValue + bottomBarHeight.intValue).toDp()
                }
                val bottomReserve = if (measuredBottomReserve > 0.dp) {
                    measuredBottomReserve
                } else {
                    coverArtViewportReserve
                }
                (viewportHeight - topPadding - bottomPadding - topBarReserve - bottomReserve)
                    .coerceAtLeast(0.dp)
            }
            val coverViewportWidth = maxWidth
            val coverMaxSize = remember(coverViewportWidth, coverSlotHeight) {
                val viewportWidth = coverViewportWidth - (songCoverHorizontalPadding * 2).dp
                minOf(
                    maxSongCoverSize.dp,
                    viewportWidth.coerceAtLeast(0.dp),
                    (coverSlotHeight - (songCoverVerticalPadding * 2).dp).coerceAtLeast(0.dp),
                )
            }
            val coverVerticalPadding = remember(coverSlotHeight, coverMaxSize) {
                val basePadding = songCoverVerticalPadding.dp
                ((coverSlotHeight - coverMaxSize) / 2).coerceAtLeast(basePadding)
            }
            LazyColumn(
                Modifier
                    .onSizeChanged {
                        heightState.intValue = it.height
                    }
                    .paddingMask(safeDrawing) {
                        playerSheet?.progressState?.floatValue ?: 0f
                    },
                state = listState,
                contentPadding = PaddingValues(
                    top = topPadding,
                    start = safeDrawing.calculateStartPadding(layoutDirection),
                    end = safeDrawing.calculateEndPadding(layoutDirection),
                )
            ) {
                item {
                    TopBar(i, transformModifier) { topBarHeight.intValue = it }
                }
                item {
                    AnimatedContent(
                        targetState = showLyrics,
                        modifier = Modifier.fillMaxWidth().height(coverSlotHeight),
                        contentAlignment = Alignment.Center,
                        transitionSpec = {
                            val animation = tween<Float>(
                                LyricsModeTransitionDurationMs,
                                easing = FastOutSlowInEasing
                            )
                            val enter = fadeIn(animation) + scaleIn(animation, 0.96f)
                            val exit = fadeOut(tween(LyricsModeTransitionDurationMs / 2)) +
                                    scaleOut(animation, 1.04f)
                            enter togetherWith exit
                        }
                    ) { lyricsVisible ->
                        if (lyricsVisible) {
                            LyricsPanel(
                                lyrics = niceLyrics,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopStart
                            ) {
                                CoverArt(
                                    i,
                                    paletteState,
                                    coverMaxSize,
                                    coverVerticalPadding,
                                    topPadding,
                                    coverViewportWidth
                                )
                            }
                        }
                    }
                }
                item {
                    Box(transformModifier.onSizeChanged { timelineHeight.intValue = it.height }) {
                        ExpandedTimeline(i, lyricsVisible = showLyrics)
                    }
                }
                item {
                    Box(transformModifier.onSizeChanged { controllerHeight.intValue = it.height }) {
                        Controller()
                    }
                }
                stickyHeader(bottomBarKey, PlayerBottomBarContentType, isSlidable = false) {
                    Box(transformModifier.onSizeChanged { bottomBarHeight.intValue = it.height }) {
                        BottomBar(
                            index = i,
                            isSticky = isBottomBarSticky,
                            lyricsVisible = showLyrics,
                            onLyricsClick = {
                                listState.requestScrollToItem(0)
                                showLyrics = !showLyrics
                            }
                        )
                    }
                }

                val firstHeaderKey = "Header $i"
                stickyHeader(key = firstHeaderKey, contentType = "player-header") {
                    Box(transformModifier.clipHeaderTop(listState, firstHeaderKey)) {
                        Header(i.toString())
                    }
                }
                materialGroup(
                    lazyListState = listState,
                    clipPadding = PaddingValues(bottom = 8.dp + bottomPadding),
                ) {
                    (0 until PlayerQueueItemsPerGroup).forEach {
                        card(
                            modifier = transformModifier.padding(horizontal = 8.dp),
                            key = "$i$it",
                            contentType = PlayerQueueItemContentType,
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

                val secondHeaderKey = "Header $i-2"
                stickyHeader(key = secondHeaderKey, contentType = "player-header-2") {
                    Box(transformModifier.clipHeaderTop(listState, secondHeaderKey)) {
                        Header("$i 2")
                    }
                }
                materialGroup(
                    lazyListState = listState,
                    clipPadding = PaddingValues(bottom = 8.dp + bottomPadding),
                ) {
                    (0 until PlayerQueueItemsPerGroup).forEach {
                        card(
                            modifier = transformModifier.padding(horizontal = 8.dp),
                            key = "$i$it 2",
                            contentType = PlayerQueueItemContentType,
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

            CollapsedPlayer(
                i = i,
                showCover = showLyrics,
            )
            val scrollbarItemSizes = remember { PlayerScrollbarItemSizes() }
            val scrollbarState = rememberPlayerScrollbarState(listState, scrollbarItemSizes)
            FastScrollbar(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .padding(top = 4.dp, bottom = 4.dp)
                    .padding(safeDrawing)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                        alpha = playerSheet?.progressState?.floatValue ?: 1f
                    }
                    .align(Alignment.TopEnd),
                state = scrollbarState,
                scrollInProgress = listState.isScrollInProgress,
                orientation = Orientation.Vertical,
                onThumbMoved = rememberPlayerScrollbarThumbMover(listState, scrollbarItemSizes)
            )
        }
    }
}

private fun Modifier.clipHeaderTop(
    lazyListState: LazyListState,
    key: Any,
) = drawWithContent {
    val layoutInfo = lazyListState.layoutInfo
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
    val clipTop = item?.let { layoutInfo.beforeContentPadding - it.offset } ?: 0
    if (clipTop <= 0) drawContent()
    else clipRect(top = clipTop.toFloat().coerceAtMost(size.height)) {
        this@drawWithContent.drawContent()
    }
}

fun Modifier.expandedListItemTransform(
    playerSheet: BetterSheet?,
    playerHeight: () -> Int
): Modifier {
    return graphicsLayer {
        val sheetProgress = playerSheet?.progressState?.floatValue ?: 0f
        val positiveProgress = sheetProgress.coerceIn(0f, 1f)
        val offset = 1 - positiveProgress
        alpha = if (positiveProgress > 0.75f) (positiveProgress - 0.75f) * 4 else 0f
        translationY = offset * playerHeight()
    }
}

@Composable
private fun RepeatButton(modifier: Modifier = Modifier) {
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
private fun PreviousButton(modifier: Modifier = Modifier, large: Boolean = false) {
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
private fun PlayPauseButton(modifier: Modifier = Modifier, large: Boolean = false) {
    val controls = LocalPlayerControls.current
    val isPlaying = controls?.isPlaying == true
    FilledIconButton(
        onClick = { controls?.isPlaying = !isPlaying },
        enabled = controls != null,
        modifier = modifier,
        shapes = IconButtonDefaults.shapes()
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
private fun NextButton(modifier: Modifier = Modifier, large: Boolean = false) {
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
private fun ShuffleButton(modifier: Modifier = Modifier) {
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
fun PlayerButtons() = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    modifier = Modifier.clip(RoundedCornerShape(100))
        .background(colorScheme.primary.copy(0.25f))
        .padding(horizontal = 4.dp)
) {
    PreviousButton(Modifier.size(40.dp))
    PlayPauseButton(Modifier.size(48.dp))
    NextButton(Modifier.size(40.dp))
}


@Composable
fun Controller() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(8.dp).padding(bottom = 8.dp)
    ) {
        RepeatButton(Modifier.size(48.dp))
        PreviousButton(Modifier.size(56.dp), large = true)
        PlayPauseButton(Modifier.size(56.dp), large = true)
        NextButton(Modifier.size(56.dp), large = true)
        ShuffleButton(Modifier.size(48.dp))
    }
}

@Composable
fun ExpandedTimeline(
    i: Int,
    lyricsVisible: Boolean = false
) = Box {
    val timelineState = LocalPlayerTimelineState.current ?: return@Box
    val image = LocalPlayerItems.current[i]
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
    val heightState = remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val mergedStyle = LocalTextStyle.current.merge(typography.labelLarge)

    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        PlayerSlider(
            Modifier.weight(1f).padding(top = heightState.value),
            Modifier.height(72.dp).padding(vertical = 20.dp),
            timelineState
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.onSizeChanged {
                heightState.value = density.run { it.height.toDp() } - 12.dp
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(lyricsVisible) {
                BetterImage(
                    model = { image },
                    contentDescription = "Song $i artwork",
                    modifier = Modifier
                        .padding(start = 16.dp, end = 8.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.primaryFixed)
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
            Spacer(Modifier.width(8.dp))
            MoreButton()
            Spacer(Modifier.width(8.dp))
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Text(
                text = currentTimeLabel,
                style = mergedStyle,
                modifier = Modifier.padding(8.dp)
            )
            Spacer(Modifier.weight(1f))
            val endInteraction = remember { MutableInteractionSource() }
            Text(
                text = endLabel,
                style = mergedStyle,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .clickable(interactionSource = endInteraction) {
                        showRemainingTime = !showRemainingTime
                    }.padding(8.dp)
            )
        }
    }
    Text(
        text = "FLAC",
        style = mergedStyle,
        modifier = Modifier
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(100))
            .background(colorScheme.primary.copy(0.1f))
            .clickable {}
            .padding(12.dp, 4.dp)
            .align(Alignment.BottomCenter)
    )
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
fun LyricsToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    FilledTonalIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.size(48.dp),
        shapes = IconButtonDefaults.toggleableShapes(
            checkedShape = RoundedCornerShape(100)
        ),
        interactionSource = interactionSource,
        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
            Color.Transparent,
            colorScheme.onPrimaryContainer,
            checkedContainerColor = Color.Transparent,
            checkedContentColor = colorScheme.onPrimaryContainer,
        ),
    ) {
        Icon(
            painterResource(Res.drawable.ic_mic_music_3),
            contentDescription = if (checked) "Hide lyrics" else "Show lyrics"
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

@Composable
fun CoverArt(
    i: Int,
    paletteState: (Palette) -> Unit,
    maxCoverSize: Dp,
    verticalPadding: Dp,
    topPadding: Dp,
    viewportWidth: Dp,
) {
    val playerSheet = LocalPlayerSheet.current
    val playerPadding = LocalPlayerPadding.current

    val layoutDirection = LocalLayoutDirection.current
    val artWorks = LocalPlayerItems.current
    val image = artWorks[i]

    val animatedTargetX = animateDpAsState(
        playerPadding.calculateStartPadding(layoutDirection) + (collapsedHorizontalPadding + 8).dp,
        tween()
    )
    BetterImage(
        { image },
        "Song $i",
        Modifier
            .coverSize(maxCoverSize, verticalPadding)
            .graphicsLayer {
                val sheetProgress = playerSheet?.progressState?.floatValue ?: 0f
                val positiveProgress = sheetProgress.coerceIn(0f, 1f)
                val offset = 1 - positiveProgress

                val targetX = animatedTargetX.value.toPx()
                val targetY = -(playerSheet?.peekHeight?.toPx() ?: 0f) / 2
                val targetSize = 48.dp
                val targetScale = targetSize.toPx() / size.height
                scaleX = 1 + (targetScale - 1) * offset
                scaleY = scaleX
                transformOrigin = TransformOrigin(0f, 0f)
                val center = (viewportWidth.toPx() - size.width) / 2f
                translationX =
                    -songCoverHorizontalPadding.dp.toPx() + targetX * offset + center * positiveProgress

                val collapsedY = targetY - verticalPadding.toPx() - topPadding.toPx()
                translationY = collapsedY * offset

                clip = true
                shape = RoundedCornerShape((8 / scaleX).dp)
            }
            .background(colorScheme.primaryFixed),
        PalettePlugin { paletteState(it) }
    )
}

@Composable
fun TopBar(
    index: Int,
    modifier: Modifier = Modifier,
    onHeightChanged: (Int) -> Unit
) {
    val playerSheet = LocalPlayerSheet.current
    val sheetState = playerSheet?.sheetState
    val scope = rememberCoroutineScope()
    Row(modifier.padding(end = 8.dp).onSizeChanged { onHeightChanged(it.height) }) {
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
private fun LyricsPanel(
    lyrics: Lyrics.Word,
    modifier: Modifier = Modifier
) {
    val timelineState = LocalPlayerTimelineState.current ?: return
    FullTimedLyrics(
        lyrics = lyrics,
        positionMs = timelineState.positionMs.toLong(),
        onSeek = { positionMs ->
            timelineState.positionMs = positionMs
                .toFloat()
                .coerceIn(0f, timelineState.durationMs)
        },
        modifier = modifier.padding(horizontal = 12.dp)
    )
}

@Composable
private fun FullTimedLyrics(
    lyrics: Lyrics.Word,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var hasInitialLyricsPosition by remember(lyrics.lines) { mutableStateOf(false) }
    var lyricsViewportHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val verticalContentPadding = if (lyricsViewportHeight > 0)
        with(density) { (lyricsViewportHeight / 2f).toDp() }
    else 64.dp
    val currentLineIndex = remember(lyrics.lines, positionMs) {
        lyrics.lines.indexOfLast { it.startMs <= positionMs }.coerceAtLeast(0)
    }

    LaunchedEffect(currentLineIndex, lyrics.lines, lyricsViewportHeight) {
        if (lyrics.lines.isEmpty() || lyricsViewportHeight == 0) return@LaunchedEffect

        val targetWasVisible = listState.layoutInfo.visibleItemsInfo
            .any { it.index == currentLineIndex }
        if (!targetWasVisible) {
            listState.requestScrollToItem(currentLineIndex)
        }

        val targetItem = snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == currentLineIndex }
        }.first { it != null } ?: return@LaunchedEffect

        withFrameNanos { }
        val centerOffset = targetItem.size / 2
        val shouldAnimate = hasInitialLyricsPosition && targetWasVisible
        if (shouldAnimate) listState.animateScrollToItem(currentLineIndex, centerOffset)
        else listState.requestScrollToItem(currentLineIndex, centerOffset)
        hasInitialLyricsPosition = true
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { lyricsViewportHeight = it.height }
            .lyricsEdgeFade(),
        contentPadding = PaddingValues(vertical = verticalContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = lyrics.lines,
            key = { index, line -> "${line.startMs}-$index" }
        ) { lineIndex, line ->
            FullTimedLyricsLine(
                line = line,
                positionMs = positionMs,
                isActive = lineIndex == currentLineIndex &&
                        positionMs in line.startMs..line.endMs,
                onClick = { onSeek(line.startMs) }
            )
        }
    }
}

private fun Modifier.lyricsEdgeFade(fadeHeight: Dp = 64.dp): Modifier =
    graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }.drawWithCache {
        val edgeFraction = if (size.height > 0f) {
            (fadeHeight.toPx() / size.height).coerceIn(0f, 0.5f)
        } else 0f
        val mask = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                edgeFraction to Color.Black,
                (1f - edgeFraction) to Color.Black,
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
private fun FullTimedLyricsLine(
    line: WordsLyric,
    positionMs: Long,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val lineAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.32f,
        animationSpec = tween(360, easing = FastOutSlowInEasing)
    )
    val lyricColor = colorScheme.onPrimaryContainer
    val lyricText = remember(line, positionMs, isActive, lyricColor) {
        buildAnnotatedString {
            line.tokens.forEach { token ->
                val tokenAlpha = if (isActive) {
                    val fadeProgress = ((positionMs -
                            (token.startMs - LyricsWordFadeLeadMs)).toFloat() /
                            LyricsWordFadeLeadMs).coerceIn(0f, 1f)
                    0.42f + 0.58f * FastOutSlowInEasing.transform(fadeProgress)
                } else {
                    1f
                }
                withStyle(
                    SpanStyle(
                        color = lyricColor.copy(alpha = tokenAlpha),
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(token.text)
                    append(token.trailingSpace)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = lineAlpha
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = lyricText,
            modifier = Modifier.fillMaxWidth(),
            style = typography.titleLarge,
            lineHeight = typography.headlineSmall.lineHeight
        )
        line.backgroundVocals.takeIf { it.isNotEmpty() }?.let { vocals ->
            Text(
                text = vocals.joinToString("") { it.text + it.trailingSpace },
                modifier = Modifier.fillMaxWidth(),
                style = typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                color = lyricColor.copy(alpha = 0.72f)
            )
        }
        line.translations.forEach { translation ->
            Text(
                text = translation.text,
                modifier = Modifier.fillMaxWidth(),
                style = typography.bodyLarge,
                color = lyricColor.copy(alpha = 0.78f)
            )
        }
    }
}


@Composable
fun BottomBar(
    index: Int = 0,
    isSticky: Boolean = false,
    lyricsVisible: Boolean = false,
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
            .padding(8.dp)
    ) {
        Crossfade(
            targetState = isSticky,
            animationSpec = tween()
        ) { sticky ->
            if (sticky) StickyMiniPlayer(index)
            else LyricsBottomBar(lyricsVisible, onLyricsClick)
        }
    }
}

@Composable
private fun LyricsBottomBar(
    lyricsVisible: Boolean,
    onLyricsClick: () -> Unit
) {
    val timelineState = LocalPlayerTimelineState.current ?: return
    Row(
        Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LyricsToggle(
            checked = lyricsVisible,
            onCheckedChange = { onLyricsClick() }
        )
        Box(
            Modifier.height(48.dp)
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(colorScheme.primary.copy(0.1f))
                .clickable(onClick = onLyricsClick)
        ) {
            Crossfade(
                targetState = lyricsVisible,
                animationSpec = tween(LyricsTransitionDurationMs)
            ) { modeVisible ->
                if (modeVisible) Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        text = "Lyrics",
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else TimedLyricsTicker(
                    lyrics = niceLyrics,
                    positionMs = timelineState.positionMs.toLong()
                )
            }
        }
        Icon(
            painterResource(Res.drawable.ic_queue_music),
            modifier = Modifier.size(48.dp).padding(12.dp),
            contentDescription = null
        )
    }
}

private data class LyricsTickerContent(
    val lineIndex: Int?,
    val order: Int
)

private fun lyricsTickerContent(
    lines: List<WordsLyric>,
    positionMs: Long
): LyricsTickerContent {
    val previousIndex = lines.indexOfLast { it.startMs <= positionMs }
    val previousLine = lines.getOrNull(previousIndex)
    if (previousLine != null && positionMs <= previousLine.endMs) {
        return LyricsTickerContent(lineIndex = previousIndex, order = previousIndex * 2)
    }

    val nextIndex = previousIndex + 1
    val nextLine = lines.getOrNull(nextIndex)
    val gapMs = if (previousLine == null) nextLine?.startMs ?: Long.MAX_VALUE
    else (nextLine?.startMs ?: Long.MAX_VALUE) - previousLine.endMs
    return if (nextLine != null && gapMs < LyricsWaitingGapMs)
        LyricsTickerContent(lineIndex = nextIndex, order = nextIndex * 2)
    else
        LyricsTickerContent(lineIndex = null, order = previousIndex * 2 + 1)
}

@Composable
private fun TimedLyricsTicker(
    lyrics: Lyrics.Word,
    positionMs: Long
) {
    val content = remember(lyrics.lines, positionMs) {
        lyricsTickerContent(lyrics.lines, positionMs)
    }
    AnimatedContent(
        targetState = content,
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        transitionSpec = {
            val direction = if (targetState.order >= initialState.order) 1 else -1
            val animation = tween<IntOffset>(
                durationMillis = LyricsTransitionDurationMs,
                easing = FastOutSlowInEasing
            )
            val enter = slideInVertically(animation) { direction * it } +
                    fadeIn(tween(LyricsTransitionDurationMs))
            val exit = slideOutVertically(animation) { -direction * it } +
                    fadeOut(tween(LyricsTransitionDurationMs))
            enter togetherWith exit
        }
    ) { target ->
        val line = target.lineIndex?.let(lyrics.lines::getOrNull)
        if (line == null) LyricsWaitingDots()
        else TimedLyricsLine(line, positionMs)
    }
}

@Composable
private fun LyricsWaitingDots() {
    val transition = rememberInfiniteTransition()
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing)
        )
    )
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val pulse = (cos(phase - index * 2f * PI.toFloat() / 3f) + 1f) / 2f
            Box(
                Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        scaleX = 0.55f + pulse * 0.45f
                        scaleY = scaleX
                    }
                    .clip(Circle.toShape())
                    .background(
                        colorScheme.onPrimaryContainer.copy(alpha = 0.55f + pulse * 0.4f)
                    )
            )
        }
    }
}

@Composable
private fun TimedLyricsLine(
    line: WordsLyric,
    positionMs: Long
) {
    if (line.tokens.isEmpty()) return
    val activeTokenIndex = remember(line, positionMs) {
        line.tokens.indexOfLast { it.startMs <= positionMs }
    }
    val highlightedColor = colorScheme.onPrimaryContainer
    val upcomingAlpha = 0.55f
    val text = remember(line, activeTokenIndex, positionMs, highlightedColor) {
        buildAnnotatedString {
            line.tokens.forEachIndexed { index, token ->
                val fadeProgress = ((positionMs -
                        (token.startMs - LyricsWordFadeLeadMs)).toFloat() /
                        LyricsWordFadeLeadMs).coerceIn(0f, 1f)
                val easedFadeProgress = FastOutSlowInEasing.transform(fadeProgress)
                val tokenAlpha = upcomingAlpha + (1f - upcomingAlpha) * easedFadeProgress
                withStyle(
                    SpanStyle(
                        color = highlightedColor.copy(alpha = tokenAlpha),
                        fontWeight = if (index <= activeTokenIndex) FontWeight.Bold
                        else FontWeight.Normal
                    )
                ) {
                    append(token.text)
                    append(token.trailingSpace)
                }
            }
        }
    }
    val tokenOffsets = remember(line) {
        var offset = 0
        line.tokens.map { token ->
            offset.also { offset += token.text.length + token.trailingSpace.length }
        }
    }
    var textLayout by remember(line) { mutableStateOf<TextLayoutResult?>(null) }
    val layout = textLayout
    val focusTokenIndex = activeTokenIndex.coerceAtLeast(0)
        .coerceAtMost(line.tokens.lastIndex)
    val targetFocusX = if (layout == null || tokenOffsets.isEmpty()) 0f
    else layout.tokenCenter(line, tokenOffsets, focusTokenIndex)
    val focusX by animateFloatAsState(
        targetValue = targetFocusX,
        animationSpec = tween()
    )

    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            Text(
                text = text,
                style = typography.titleMedium,
                maxLines = 1,
                softWrap = false,
                onTextLayout = { textLayout = it }
            )
        }
    ) { measurables, constraints ->
        val textPlaceable = measurables.single().measure(
            Constraints(
                maxWidth = Constraints.Infinity,
                maxHeight = constraints.maxHeight
            )
        )
        val fitsWithoutScrolling = textPlaceable.width <= constraints.maxWidth
        val textX = if (fitsWithoutScrolling)
            (constraints.maxWidth - textPlaceable.width) / 2
        else (constraints.maxWidth / 2f - focusX).roundToInt()
        layout(constraints.maxWidth, constraints.maxHeight) {
            textPlaceable.placeRelative(
                x = textX,
                y = (constraints.maxHeight - textPlaceable.height) / 2
            )
        }
    }
}

private fun TextLayoutResult.tokenCenter(
    line: WordsLyric,
    tokenOffsets: List<Int>,
    tokenIndex: Int
): Float {
    val token = line.tokens[tokenIndex]
    val startOffset = tokenOffsets[tokenIndex].coerceAtMost(layoutInput.text.lastIndex)
    val endOffset = (startOffset + token.text.length - 1)
        .coerceIn(startOffset, layoutInput.text.lastIndex)
    return (getBoundingBox(startOffset).left + getBoundingBox(endOffset).right) / 2f
}

@Composable
private fun StickyMiniPlayer(
    index: Int,
) {
    val artWorks = LocalPlayerItems.current
    val image = artWorks[index]
    val playerSheet = LocalPlayerSheet.current
    val sheetState = playerSheet?.sheetState
    val scope = rememberCoroutineScope()

    CollapsedPlayerContent(
        index,
        modifier = Modifier.fillMaxWidth(),
        leadingContent = {
            BetterImage(
                { image },
                "Song $index",
                Modifier
                    .padding(start = 4.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.primaryFixed)
            )
        },
        trailingContent = {
            val interactionSource = remember { MutableInteractionSource() }
            IconButton(
                onClick = { scope.launch { sheetState?.show() } },
                interactionSource = interactionSource,
                modifier = Modifier.size(40.dp),
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
    val image = LocalPlayerItems.current[i]

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
                    BetterImage(
                        model = { image },
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
    val maxWidth = remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
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
            leadingContent()
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
            PlayPauseButton(Modifier.size(48.dp))
        }

        item(670.dp) {
            RepeatButton(Modifier.size(40.dp))
        }

        item(playButtonShow) { PlayerButtons() }
        item(710.dp) {
            ShuffleButton(Modifier.size(40.dp))
        }

        item(0.dp) {
            trailingContent()
        }
    }
}

fun formatTime(ms: Float): String {
    val totalSeconds = (ms / 1000).toInt().coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Composable
fun RowScope.Timeline() {
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
    Text(
        text = currentTimeLabel,
        style = mergedStyle,
        modifier = Modifier.widthIn(min = 40.dp),
        textAlign = TextAlign.Center
    )

    PlayerSlider(Modifier.weight(1f), timelineState = timelineState)

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
private fun PlayerSlider(
    modifier: Modifier = Modifier,
    thumbModifier: Modifier = Modifier,
    timelineState: PlayerTimelineState
) {
    val rangeMS = remember(timelineState.durationMs) { 0f..timelineState.durationMs }
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isDragged) {
        timelineState.isSeeking = isDragged
    }
    Slider(
        valueRange = rangeMS,
        value = timelineState.positionMs,
        onValueChange = { newValue ->
            timelineState.isSeeking = true
            timelineState.positionMs = newValue
        },
        onValueChangeFinished = {
            timelineState.isSeeking = false
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
            PlayerSliderTrack { it }
        }
    )
}

@Composable
private fun PlayerSliderTrack(sliderState: () -> SliderState) {
    LinearWavyProgressIndicator(
        progress = { sliderState().coercedValueAsFraction },
        modifier = Modifier.fillMaxWidth().height(16.dp),
        color = colorScheme.primary,
        trackColor = colorScheme.primary.copy(0.25f),
        amplitude = { 0.66f }
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
                modifier = Modifier.width(96.dp).padding(horizontal = 8.dp),
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
