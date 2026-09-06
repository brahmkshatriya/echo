package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.brahmkshatriya.echo.app.ui.components.BetterSheet
import dev.brahmkshatriya.echo.app.ui.components.BetterSheetScaffold
import dev.brahmkshatriya.echo.app.ui.components.blurFadePagerTransition
import dev.brahmkshatriya.echo.app.ui.components.paddingMask
import com.skydoves.landscapist.core.ImageRequest
import com.skydoves.landscapist.core.scheduler.DecodePriority
import com.skydoves.landscapist.image.getLandscapist
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

val LocalPlayerPadding = compositionLocalOf { PaddingValues.Zero }
val LocalPlayerSheet = staticCompositionLocalOf<BetterSheet?> { null }
val LocalInitialPlayerSheetValue = staticCompositionLocalOf { SheetValue.PartiallyExpanded }

@Stable
class PlayerControlsState {
    var isPlaying by mutableStateOf(false)
    var repeatEnabled by mutableStateOf(false)
    var shuffleEnabled by mutableStateOf(false)
}

val LocalPlayerPagerState = staticCompositionLocalOf<PagerState?> { null }
val LocalPlayerControls = staticCompositionLocalOf<PlayerControlsState?> { null }
val LocalPlayerLyricsVisible = staticCompositionLocalOf<androidx.compose.runtime.MutableState<Boolean>?> { null }

@Composable
fun ProvidePlayerControls(
    pagerState: PagerState,
    content: @Composable () -> Unit,
) {
    val controls = remember { PlayerControlsState() }
    val lyricsVisible = remember { mutableStateOf(false) }
    PreloadAdjacentPlayerArtwork(pagerState)
    CompositionLocalProvider(
        LocalPlayerPagerState provides pagerState,
        LocalPlayerControls provides controls,
        LocalPlayerLyricsVisible provides lyricsVisible,
        content = content,
    )
}

val LocalPlayerItems = staticCompositionLocalOf {
    listOf(
        "https://i1.sndcdn.com/artworks-f5P5EvBt5Qu57jLk-UNArNA-t1080x1080.jpg",
        "https://i1.sndcdn.com/artworks-mJmURREt59PyaXxx-nhowNw-t1080x1080.png",
        "https://i1.sndcdn.com/artworks-GzqTFOMbFiXRz5LL-G1R9uA-t1080x1080.jpg",
        "https://i1.sndcdn.com/artworks-UbVxfud5u7hzFUPc-pxSyCg-t1080x1080.png",
        "https://i1.sndcdn.com/artworks-7C8GJbswfVyxJ0z6-r5FPkQ-t1080x1080.png"
    )
}

@Composable
private fun PreloadAdjacentPlayerArtwork(pagerState: PagerState) {
    val artworks = LocalPlayerItems.current
    val landscapist = getLandscapist()
    val requestSize = with(LocalDensity.current) {
        maxSongCoverSize.dp.roundToPx().coerceAtLeast(1)
    }

    LaunchedEffect(pagerState, artworks, landscapist, requestSize) {
        snapshotFlow { pagerState.currentPage }.collectLatest { currentPage ->
            val adjacentPages = listOf(currentPage - 1, currentPage + 1)
                .filter { it in artworks.indices }

            coroutineScope {
                adjacentPages.forEach { page ->
                    launch {
                        landscapist.load(
                            ImageRequest.builder()
                                .model(artworks[page])
                                .size(requestSize, requestSize)
                                .priority(DecodePriority.LOW)
                                .build()
                        ).collect { }
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.applyPlayerTranslation() = run {
    val playerSheet = LocalPlayerSheet.current ?: return@run this
    val thresholdPx = with(LocalDensity.current) { playerSheet.peekHeight.toPx() / 2f }
    graphicsLayer {
        val y = playerSheet.offsetState.floatValue - playerSheet.midPointState.intValue
        translationY = if (y <= thresholdPx) y else (2f * thresholdPx - y).coerceAtLeast(0f)
    }
}

@Composable
fun PlayerBottomSheet(
    betterSheet: BetterSheet,
    startPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {

    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val actualBottomPadding = safePadding.calculateBottomPadding() + bottomPadding

    val layoutDirection = LocalLayoutDirection.current
    val startPadding = startPadding + safePadding.calculateStartPadding(layoutDirection)
    val endPadding = safePadding.calculateEndPadding(layoutDirection)

    CompositionLocalProvider(
        LocalPlayerSheet provides betterSheet,
        LocalPlayerPadding provides remember(startPadding, endPadding, bottomPadding) {
            PaddingValues(start = startPadding, end = endPadding, bottom = bottomPadding)
        }
    ) {
        val artWorks = LocalPlayerItems.current
        val pagerState = rememberPagerState(2, pageCount = { artWorks.size })
        val pageScrolledToTop = remember { mutableStateMapOf<Int, Boolean>() }
        val pageLyricsSelectorOpen = remember { mutableStateMapOf<Int, Boolean>() }
        val lyricsSelectorOpen by remember {
            derivedStateOf {
                pageLyricsSelectorOpen[pagerState.currentPage] == true
            }
        }
        val pagerUserScrollEnabled by remember {
            derivedStateOf {
                !lyricsSelectorOpen && pageScrolledToTop[pagerState.currentPage] != false
            }
        }

        val modifier = Modifier.graphicsLayer {
            val backProgress = betterSheet.backProgressState.floatValue
            val scale = 1 - 0.15f * backProgress
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0.5f, 1f)
        }
        BetterSheetScaffold(
            sheetContent = {
                Box(modifier.fillMaxSize().graphicsLayer {
                    val sheetProgress by betterSheet.progressState
                    alpha = 1 + sheetProgress.coerceIn(-1f, 0f)
                }) {
                    ProvidePlayerControls(pagerState) {
                        HorizontalPager(
                            pagerState,
                            Modifier.fillMaxSize(),
                            userScrollEnabled = pagerUserScrollEnabled,
                        ) { page ->
                            Box(Modifier.fillMaxSize().blurFadePagerTransition(pagerState, page) {
                                betterSheet.progressState.floatValue.coerceIn(0f, 1f)
                            }) {
                                PlayerItem(
                                    i = page,
                                    onScrolledToTopChanged = { scrolledToTop ->
                                        pageScrolledToTop[page] = scrolledToTop
                                    },
                                    onLyricsSelectorOpenChanged = { open ->
                                        pageLyricsSelectorOpen[page] = open
                                    },
                                )
                            }
                        }
                    }
                }
            },
            bottomPadding = actualBottomPadding,
            betterSheet = betterSheet,
            sheetShape = RectangleShape,
            sheetDragHandle = null,
            sheetShadowElevation = 0.dp,
            sheetContainerColor = Color.Unspecified,
            sheetMaxWidth = Dp.Unspecified,
            sheetSwipeEnabled = !lyricsSelectorOpen,
            containerColor = Color.Unspecified,
            content = {
                Box(Modifier.paddingMask().then(modifier).applyPlayerTranslation()) {
                    content()
                }
            }
        )
    }
}
