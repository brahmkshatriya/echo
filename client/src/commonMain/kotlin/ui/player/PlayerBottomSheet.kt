package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemGestures
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.core.ImageRequest
import com.skydoves.landscapist.core.model.ImageResult
import com.skydoves.landscapist.core.scheduler.DecodePriority
import com.skydoves.landscapist.image.convertToImageBitmap
import com.skydoves.landscapist.image.getLandscapist
import dev.brahmkshatriya.echo.app.ui.components.BetterSheet
import dev.brahmkshatriya.echo.app.ui.components.BetterSheetScaffold
import dev.brahmkshatriya.echo.app.ui.components.blurFadePagerTransition
import dev.brahmkshatriya.echo.app.ui.components.paddingMask
import dev.brahmkshatriya.echo.app.ui.player.song.PlayerPillTransition
import dev.brahmkshatriya.echo.app.ui.player.song.SharedPlayerQueueOverlay
import dev.brahmkshatriya.echo.app.ui.player.song.maxSongCoverSize
import dev.brahmkshatriya.echo.app.ui.player.song.rememberPlayerPillTransition
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.abs

val LocalPlayerPadding = compositionLocalOf { PaddingValues.Zero }
val LocalPlayerSheet = staticCompositionLocalOf<BetterSheet?> { null }
val LocalInitialPlayerSheetValue = staticCompositionLocalOf { SheetValue.PartiallyExpanded }

data class PlayerQueue(
    val id: Int,
    val title: String,
    val songs: List<Int>,
    val unshuffledOrder: List<Int> = songs,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
)

@Stable
class PlayerControlsState(songCount: Int) {
    var isPlaying by mutableStateOf(false)
    var radioEnabled by mutableStateOf(false)
    var currentSong by mutableIntStateOf(0)

    // Fixed demo groups and playback order: no per-launch queue generation.
    private val initialGroups = if (songCount == exampleSongs.size) {
        exampleQueueGroups
    } else listOf(ExampleQueueGroup("Sample Playlist", songCount))
    var queues by mutableStateOf(buildList {
        var nextSong = 0
        initialGroups.forEachIndexed { index, group ->
            val songs = (nextSong until nextSong + group.size).toList()
            add(PlayerQueue(index, group.title, songs))
            nextSong += group.size
        }
    })
        private set

    val playbackOrder: List<Int> get() = queues.flatMap { it.songs }
    fun songAt(page: Int): Int = playbackOrder.getOrElse(page) { -1 }
    fun pageOf(song: Int): Int = playbackOrder.indexOf(song)
    fun queueFor(song: Int): PlayerQueue? = queues.firstOrNull { song in it.songs }

    val shuffleEnabled: Boolean get() = queueFor(currentSong)?.shuffleEnabled == true
    var repeatMode: RepeatMode
        get() = queueFor(currentSong)?.repeatMode ?: RepeatMode.Off
        set(value) {
            updateQueue(currentSong) { it.copy(repeatMode = value) }
        }

    private fun updateQueue(song: Int, transform: (PlayerQueue) -> PlayerQueue) {
        queues = queues.map { queue ->
            if (song in queue.songs) transform(queue) else queue
        }
    }

    fun setShuffle(enabled: Boolean, currentSong: Int) {
        updateQueue(currentSong) { queue ->
            if (queue.shuffleEnabled == enabled) queue else {
                val reordered = if (enabled) {
                    val remaining = queue.unshuffledOrder.filterNot { it == currentSong }
                    val shuffled = remaining.shuffled()
                    val next = if (remaining.size > 1 && shuffled == remaining) shuffled.reversed() else shuffled
                    listOf(currentSong) + next
                } else queue.unshuffledOrder
                queue.copy(songs = reordered, shuffleEnabled = enabled)
            }
        }
    }

    /** Radio only changes the upcoming song within the playing queue. */
    fun selectRadioNext(song: Int) {
        updateQueue(song) { queue ->
            val position = queue.songs.indexOf(song)
            if (position < 0 || queue.songs.size <= 2) return@updateQueue queue
            val nextIndex = if (position == queue.songs.lastIndex) {
                if (queue.repeatMode == RepeatMode.Off) return@updateQueue queue else 0
            } else position + 1
            // Rotate a subsequent song into the next slot predictably. Radio
            // should not randomly reshuffle the sample queue on every run.
            val candidateIndex = (nextIndex + 1) % queue.songs.size
            if (candidateIndex == position || candidateIndex == nextIndex) queue else {
                val songs = queue.songs.toMutableList()
                songs[candidateIndex] = songs[nextIndex]
                songs[nextIndex] = queue.songs[candidateIndex]
                queue.copy(songs = songs, unshuffledOrder = if (queue.shuffleEnabled) queue.unshuffledOrder else songs)
            }
        }
    }

    /** The target is a global playback index, but it must belong to the same queue. */
    fun moveQueueSong(fromSong: Int, toIndex: Int) {
        val target = songAt(toIndex)
        val fromQueue = queueFor(fromSong) ?: return
        if (queueFor(target)?.id != fromQueue.id) return
        updateQueue(fromSong) { queue ->
            val from = queue.songs.indexOf(fromSong)
            val to = queue.songs.indexOf(target)
            if (from < 0 || to < 0 || from == to) queue else {
                val songs = queue.songs.toMutableList().apply { add(to, removeAt(from)) }
                queue.copy(songs = songs, unshuffledOrder = if (queue.shuffleEnabled) queue.unshuffledOrder else songs)
            }
        }
    }

    /** Repeat is local to a queue. Only repeat-off can advance into the next queue. */
    fun nextSong(song: Int = currentSong): Int? {
        val queueIndex = queues.indexOfFirst { song in it.songs }
        if (queueIndex < 0) return null
        val queue = queues[queueIndex]
        val position = queue.songs.indexOf(song)
        if (position < queue.songs.lastIndex) return queue.songs[position + 1]
        if (queue.repeatMode != RepeatMode.Off) return queue.songs.firstOrNull()
        return queues.getOrNull(queueIndex + 1)?.songs?.firstOrNull()
    }

    fun previousSong(song: Int = currentSong): Int? {
        val queueIndex = queues.indexOfFirst { song in it.songs }
        if (queueIndex < 0) return null
        val queue = queues[queueIndex]
        val position = queue.songs.indexOf(song)
        if (position > 0) return queue.songs[position - 1]
        if (queue.repeatMode != RepeatMode.Off) return queue.songs.lastOrNull()
        return queues.getOrNull(queueIndex - 1)?.songs?.lastOrNull()
    }
}

enum class RepeatMode { Off, All, One }

val LocalPlayerPagerState = staticCompositionLocalOf<PagerState?> { null }
val LocalPlayerControls = staticCompositionLocalOf<PlayerControlsState?> { null }
internal val LocalPlayerSkipNavigator = staticCompositionLocalOf<PlayerSkipNavigator?> { null }
internal val LocalPlayerQueueTransition = staticCompositionLocalOf<PlayerPillTransition?> { null }
val LocalPlayerLyricsVisible =
    staticCompositionLocalOf<androidx.compose.runtime.MutableState<Boolean>?> { null }
val LocalPlayerLyricsChromeCollapsed =
    staticCompositionLocalOf<androidx.compose.runtime.MutableState<Boolean>?> { null }

@Composable
fun ProvidePlayerControls(
    pagerState: PagerState,
    controls: PlayerControlsState,
    content: @Composable () -> Unit,
) {
    val lyricsVisible = rememberSaveable { mutableStateOf(false) }
    val lyricsChromeCollapsed = rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val skipNavigator = remember(pagerState, controls) {
        PlayerSkipNavigator(pagerState, controls, scope)
    }
    // A queue opened on one song remains the same overlay on every pager page.
    val queueTransition = rememberPlayerPillTransition("queue")
    PreloadAdjacentPlayerArtwork(pagerState, controls)
    CompositionLocalProvider(
        LocalPlayerPagerState provides pagerState,
        LocalPlayerControls provides controls,
        LocalPlayerSkipNavigator provides skipNavigator,
        LocalPlayerQueueTransition provides queueTransition,
        LocalPlayerLyricsVisible provides lyricsVisible,
        LocalPlayerLyricsChromeCollapsed provides lyricsChromeCollapsed,
        content = content,
    )
}

// Preserve the artwork URL list type used by the pager and image preloader.
val LocalPlayerItems = staticCompositionLocalOf { exampleSongs.map { it.artworkUrl } }

@Composable
private fun PreloadAdjacentPlayerArtwork(pagerState: PagerState, controls: PlayerControlsState) {
    val artworks = LocalPlayerItems.current
    val landscapist = getLandscapist()
    val requestSize = with(LocalDensity.current) {
        maxSongCoverSize.dp.roundToPx().coerceAtLeast(1)
    }

    LaunchedEffect(pagerState, controls, artworks, landscapist, requestSize) {
        snapshotFlow { pagerState.currentPage to controls.playbackOrder }.collectLatest { (currentPage, order) ->
            val adjacentPages = listOf(currentPage - 1, currentPage + 1)
                .mapNotNull { order.getOrNull(it) }
                .filter { it in artworks.indices }

            coroutineScope {
                adjacentPages.forEach { page ->
                    launch {
                        val artworkModel = artworks[page]
                        landscapist.load(
                            ImageRequest.builder()
                                .model(artworkModel)
                                .size(requestSize, requestSize)
                                .priority(DecodePriority.LOW)
                                .build()
                        ).collect { result ->
                            if (result is ImageResult.Success && result.isFinal) {
                                val artwork = convertToImageBitmap(result.data) ?: return@collect
                                cachePlayerArtwork(artworkModel, artwork)
                                // Loading an image alone didn't help the first
                                // frame: the palette was generated only when
                                // the next pager page was composed. Generate
                                // it here, before Next/Previous is tapped.
                                if (cachedPlayerArtworkPalette(artworkModel) == null) {
                                    prefetchPlayerArtworkPalette(artworkModel, artwork)
                                }
                            }
                        }
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

private fun Modifier.reserveSystemGestureEdges(
    leftInsetPx: Int,
    rightInsetPx: Int,
): Modifier = pointerInput(leftInsetPx, rightInsetPx) {
    awaitEachGesture {
        val down = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Initial,
        )
        val fromLeftEdge = leftInsetPx > 0 && down.position.x <= leftInsetPx
        val fromRightEdge = rightInsetPx > 0 && down.position.x >= size.width - rightInsetPx
        if (!fromLeftEdge && !fromRightEdge) return@awaitEachGesture

        val start = down.position
        var claimedForSystemBack = false

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break

            if (!claimedForSystemBack) {
                val delta = change.position - start
                val horizontalDrag = abs(delta.x) > viewConfiguration.touchSlop &&
                        abs(delta.x) > abs(delta.y)
                val movingInward =
                    (fromLeftEdge && delta.x > 0f) || (fromRightEdge && delta.x < 0f)

                if (horizontalDrag && movingInward) {
                    claimedForSystemBack = true
                } else if (
                    abs(delta.y) > viewConfiguration.touchSlop ||
                    (abs(delta.x) > viewConfiguration.touchSlop && !movingInward)
                ) break
            }

            if (claimedForSystemBack) {
                event.changes.forEach { it.consume() }
            }
        }
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
    val density = LocalDensity.current
    val startPadding = startPadding + safePadding.calculateStartPadding(layoutDirection)
    val endPadding = safePadding.calculateEndPadding(layoutDirection)
    val systemGestureInsets = WindowInsets.systemGestures
    val leftSystemGestureInset = systemGestureInsets.getLeft(density, layoutDirection)
    val rightSystemGestureInset = systemGestureInsets.getRight(density, layoutDirection)

    CompositionLocalProvider(
        LocalPlayerSheet provides betterSheet,
        LocalPlayerPadding provides remember(startPadding, endPadding, bottomPadding) {
            PaddingValues(start = startPadding, end = endPadding, bottom = bottomPadding)
        }
    ) {
        val artWorks = LocalPlayerItems.current
        val controls = remember(artWorks.size) { PlayerControlsState(artWorks.size) }
        val pagerState = rememberPagerState(0, pageCount = { artWorks.size })
        val currentSong = controls.currentSong
        LaunchedEffect(pagerState, controls) {
            snapshotFlow { pagerState.currentPage }.collect { page ->
                controls.currentSong = controls.songAt(page)
            }
        }
        val pageScrolledToTop = remember { mutableStateMapOf<Int, Boolean>() }
        val pageInteractionLocked = remember { mutableStateMapOf<Int, Boolean>() }
        val interactionLocked by remember {
            derivedStateOf {
                pageInteractionLocked[controls.songAt(pagerState.currentPage)] == true
            }
        }
        val backGestureActive by remember {
            derivedStateOf { betterSheet.backProgressState.floatValue > 0f }
        }
        val pagerUserScrollEnabled by remember {
            derivedStateOf {
                !interactionLocked &&
                        !backGestureActive &&
                        pageScrolledToTop[controls.songAt(pagerState.currentPage)] != false
            }
        }

        LaunchedEffect(pagerState, betterSheet) {
            snapshotFlow { betterSheet.backProgressState.floatValue > 0f }
                .distinctUntilChanged()
                .filter { it }
                .collectLatest {
                    if (pagerState.isScrollInProgress || pagerState.currentPageOffsetFraction != 0f) {
                        pagerState.scrollToPage(pagerState.settledPage)
                    }
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
                BoxWithConstraints(Modifier.fillMaxSize().graphicsLayer {
                    val sheetProgress by betterSheet.progressState
                    alpha = 1 + sheetProgress.coerceIn(-1f, 0f)
                    val backProgress = betterSheet.backProgressState.floatValue
                    val scale = 1f - 0.15f * backProgress
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }) {
                    val queueViewport = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
                    ProvidePlayerControls(pagerState, controls) {
                        val queueTransition = LocalPlayerQueueTransition.current
                        val skipNavigator = LocalPlayerSkipNavigator.current
                        Box(Modifier.fillMaxSize()) {
                          HorizontalPager(
                            pagerState,
                            Modifier
                                .fillMaxSize()
                                .reserveSystemGestureEdges(
                                    leftInsetPx = leftSystemGestureInset,
                                    rightInsetPx = rightSystemGestureInset,
                                ),
                            userScrollEnabled = pagerUserScrollEnabled,
                            key = { controls.songAt(it) },
                          ) { page ->
                            val song = controls.songAt(page)
                            Box(Modifier.fillMaxSize().blurFadePagerTransition(pagerState, page) {
                                if (betterSheet.backProgressState.floatValue > 0f) 0f
                                else betterSheet.progressState.floatValue.coerceIn(0f, 1f)
                            }.animatedSkipEntrance(skipNavigator, song)) {
                                PlayerItem(
                                    i = song,
                                    onScrolledToTopChanged = { scrolledToTop ->
                                        pageScrolledToTop[song] = scrolledToTop
                                    },
                                    onInteractionLockChanged = { locked ->
                                        pageInteractionLocked[song] = locked
                                    },
                                )
                            }
                          }
                          if (queueTransition != null && (queueTransition.open || queueTransition.mounted)) {
                            PlayerItemHost(
                                index = currentSong,
                                artworkMaxSize = maxSongCoverSize.dp,
                                coloredBackground = false,
                            ) {
                                SharedPlayerQueueOverlay(
                                    transition = queueTransition,
                                    viewport = queueViewport,
                                    currentSong = currentSong,
                                    onSongSelected = { song ->
                                        skipNavigator?.selectSong(song)
                                    },
                                )
                            }
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
            sheetSwipeEnabled = !interactionLocked,
            containerColor = Color.Unspecified,
            content = {
                Box(Modifier.paddingMask().then(modifier).applyPlayerTranslation()) {
                    content()
                }
            }
        )
    }
}
