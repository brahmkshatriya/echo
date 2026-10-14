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
import dev.brahmkshatriya.echo.app.ui.components.BetterSheet
import dev.brahmkshatriya.echo.app.ui.components.BetterSheetScaffold
import dev.brahmkshatriya.echo.app.ui.components.blurFadePagerTransition
import dev.brahmkshatriya.echo.app.ui.components.paddingMask
import dev.brahmkshatriya.echo.app.ui.player.song.ExampleQueueGroup
import dev.brahmkshatriya.echo.app.ui.player.song.LocalPlayerItems
import dev.brahmkshatriya.echo.app.ui.player.song.PlayerPillTransition
import dev.brahmkshatriya.echo.app.ui.player.song.ProvideSongPlayerState
import dev.brahmkshatriya.echo.app.ui.player.song.SharedPlayerQueueOverlay
import dev.brahmkshatriya.echo.app.ui.player.song.SongPlayerArtworkHost
import dev.brahmkshatriya.echo.app.ui.player.song.SongPlayerPage
import dev.brahmkshatriya.echo.app.ui.player.song.exampleQueueGroups
import dev.brahmkshatriya.echo.app.ui.player.song.exampleSongs
import dev.brahmkshatriya.echo.app.ui.player.song.maxSongCoverSize
import dev.brahmkshatriya.echo.app.ui.player.song.rememberPlayerPillTransition
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

val LocalPlayerPadding = compositionLocalOf { PaddingValues.Zero }
val LocalPlayerSheet = staticCompositionLocalOf<BetterSheet?> { null }
val LocalNowPlayingControls = staticCompositionLocalOf<PlayerControlsState?> { null }
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
class PlayerControlsState(private val songCount: Int) {
    var isPlaying by mutableStateOf(false)
    var radioEnabled by mutableStateOf(false)
    var currentSong by mutableIntStateOf(0)
    private var favouriteSongs by mutableStateOf(emptySet<Int>())

    fun isFavourite(song: Int): Boolean = song in favouriteSongs

    fun setFavourite(song: Int, favourite: Boolean) {
        if (song !in 0 until songCount) return
        favouriteSongs = if (favourite) favouriteSongs + song else favouriteSongs - song
    }

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
                    val next =
                        if (remaining.size > 1 && shuffled == remaining) shuffled.reversed() else shuffled
                    listOf(currentSong) + next
                } else queue.unshuffledOrder
                queue.copy(songs = reordered, shuffleEnabled = enabled)
            }
        }
    }

    fun selectRadioNext(song: Int) {
        updateQueue(song) { queue ->
            val position = queue.songs.indexOf(song)
            if (position < 0 || queue.songs.size <= 2) return@updateQueue queue
            val nextIndex = if (position == queue.songs.lastIndex) {
                if (queue.repeatMode == RepeatMode.Off) return@updateQueue queue else 0
            } else position + 1
            val candidateIndex = (nextIndex + 1) % queue.songs.size
            if (candidateIndex == position || candidateIndex == nextIndex) queue else {
                val songs = queue.songs.toMutableList()
                songs[candidateIndex] = songs[nextIndex]
                songs[nextIndex] = queue.songs[candidateIndex]
                queue.copy(
                    songs = songs,
                    unshuffledOrder = if (queue.shuffleEnabled) queue.unshuffledOrder else songs
                )
            }
        }
    }

    fun moveQueueSong(fromSong: Int, toIndex: Int) {
        val target = songAt(toIndex)
        val fromQueue = queueFor(fromSong) ?: return
        if (queueFor(target)?.id != fromQueue.id) return
        updateQueue(fromSong) { queue ->
            val from = queue.songs.indexOf(fromSong)
            val to = queue.songs.indexOf(target)
            if (from < 0 || to < 0 || from == to) queue else {
                val songs = queue.songs.toMutableList().apply { add(to, removeAt(from)) }
                queue.copy(
                    songs = songs,
                    unshuffledOrder = if (queue.shuffleEnabled) queue.unshuffledOrder else songs
                )
            }
        }
    }

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

val LocalPlayerPagerState =
    staticCompositionLocalOf<PagerState?> { null }
val LocalPlayerControls =
    staticCompositionLocalOf<PlayerControlsState?> { null }
internal val LocalPlayerSkipNavigator =
    staticCompositionLocalOf<PlayerSkipNavigator?> { null }
internal val LocalPlayerQueueTransition =
    staticCompositionLocalOf<PlayerPillTransition?> { null }
@Composable
fun ProvidePlayerControls(
    pagerState: PagerState,
    controls: PlayerControlsState,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val skipNavigator = remember(pagerState, controls) {
        PlayerSkipNavigator(pagerState, controls, scope)
    }
    val queueTransition = rememberPlayerPillTransition("queue")
    CompositionLocalProvider(
        LocalPlayerPagerState provides pagerState,
        LocalPlayerControls provides controls,
        LocalPlayerSkipNavigator provides skipNavigator,
        LocalPlayerQueueTransition provides queueTransition,
        content = content,
    )
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
    val artWorks = LocalPlayerItems.current
    val controls = remember(artWorks.size) { PlayerControlsState(artWorks.size) }

    CompositionLocalProvider(
        LocalPlayerSheet provides betterSheet,
        LocalNowPlayingControls provides controls,
        LocalPlayerPadding provides remember(startPadding, endPadding, bottomPadding) {
            PaddingValues(start = startPadding, end = endPadding, bottom = bottomPadding)
        }
    ) {
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
                    val queueViewport =
                        Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
                    ProvidePlayerControls(pagerState, controls) {
                        val queueTransition = LocalPlayerQueueTransition.current
                        val skipNavigator = LocalPlayerSkipNavigator.current
                        ProvideSongPlayerState {
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
                                    val effectProgress = {
                                        if (betterSheet.backProgressState.floatValue > 0f) 0f
                                        else betterSheet.progressState.floatValue.coerceIn(0f, 1f)
                                    }
                                    Box(
                                        Modifier.fillMaxSize()
                                            .blurFadePagerTransition(pagerState, page, effectProgress)
                                            .animatedSkipEntrance(skipNavigator, song, effectProgress)
                                    ) {
                                        SongPlayerPage(
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
                                    SongPlayerArtworkHost(
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
