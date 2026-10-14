package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kmpalette.generatePalette
import com.kmpalette.palette.graphics.Palette
import com.materialkolor.ktx.animateColorScheme
import com.skydoves.landscapist.image.LandscapistImage
import com.skydoves.landscapist.image.convertToImageBitmap
import dev.brahmkshatriya.echo.app.ui.components.ScrollbarState
import dev.brahmkshatriya.echo.app.ui.components.scrollbarStateValue
import dev.brahmkshatriya.echo.app.ui.player.song.SongPlayerItem
import dev.brahmkshatriya.echo.app.ui.player.song.maxSongCoverSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private data class PlayerArtworkCacheEntry(
    val artwork: ImageBitmap? = null,
    val palette: Palette? = null,
)

private object PlayerArtworkMemoryCache {
    private const val MAX_ENTRIES = 12
    private const val MAX_PALETTE_ENTRIES = 48
    private val entries = LinkedHashMap<String, PlayerArtworkCacheEntry>()
    // Invalidate adjacent pager pages as soon as their palettes are prefetched.
    private val palettes = mutableStateMapOf<String, Palette>()

    fun getPalette(model: String): Palette? = palettes[model] ?: entries[model]?.palette

    fun get(model: String): PlayerArtworkCacheEntry? {
        val entry = entries.remove(model) ?: return null
        entries[model] = entry
        return entry
    }

    fun putArtwork(model: String, artwork: ImageBitmap) {
        put(model, (entries[model] ?: PlayerArtworkCacheEntry()).copy(artwork = artwork))
    }

    fun putPalette(model: String, palette: Palette) {
        palettes[model] = palette
        while (palettes.size > MAX_PALETTE_ENTRIES) {
            palettes.remove(palettes.keys.first())
        }
        put(model, (entries[model] ?: PlayerArtworkCacheEntry()).copy(palette = palette))
    }

    private fun put(model: String, entry: PlayerArtworkCacheEntry) {
        entries.remove(model)
        entries[model] = entry
        while (entries.size > MAX_ENTRIES) {
            val oldestKey = entries.keys.firstOrNull() ?: break
            entries.remove(oldestKey)
        }
    }
}

/** Precompute the same 64-swatch palette that a player page would create.
 * Separate palette storage keeps it available after bitmap LRU eviction. */
internal suspend fun prefetchPlayerArtworkPalette(model: String, artwork: ImageBitmap) {
    if (PlayerArtworkMemoryCache.getPalette(model) != null) return
    val palette = withContext(Dispatchers.Default) {
        artwork.generatePalette {
            maximumColorCount(64)
            clearFilters()
        }
    }
    PlayerArtworkMemoryCache.putPalette(model, palette)
}

internal fun cachedPlayerArtworkPalette(model: String): Palette? =
    PlayerArtworkMemoryCache.getPalette(model)

internal fun cachePlayerArtwork(model: String, artwork: ImageBitmap) {
    PlayerArtworkMemoryCache.putArtwork(model, artwork)
}

@Composable
fun PlayerItem(
    i: Int,
    onScrolledToTopChanged: (Boolean) -> Unit = {},
    onInteractionLockChanged: (Boolean) -> Unit = {},
) {
    PlayerItemHost(
        index = i,
        artworkMaxSize = maxSongCoverSize.dp,
    ) {
        SongPlayerItem(
            i = i,
            onScrolledToTopChanged = onScrolledToTopChanged,
            onLyricsSelectorOpenChanged = onInteractionLockChanged,
        )
    }
}

@Composable
internal fun PlayerItemHost(
    index: Int,
    artworkMaxSize: Dp,
    coloredBackground: Boolean = true,
    content: @Composable () -> Unit,
) {
    val artworkModel = LocalPlayerItems.current.getOrNull(index)
    val cachedArtwork = remember(artworkModel) {
        artworkModel?.let(PlayerArtworkMemoryCache::get)
    }
    var artwork by remember(artworkModel) { mutableStateOf(cachedArtwork?.artwork) }
    var locallyLoadedPalette by remember(artworkModel) { mutableStateOf(cachedArtwork?.palette) }
    // Prefetch may finish after an offscreen pager page was first composed.
    // Observe cache updates so that page uses the prefetched scheme immediately.
    val prefetchedPalette = artworkModel?.let(PlayerArtworkMemoryCache::getPalette)
    val palette = prefetchedPalette ?: locallyLoadedPalette
    LaunchedEffect(artworkModel, artwork, palette == null) {
        if (artworkModel != null && palette == null) {
            artwork?.let { bitmap ->
                // More swatches retain small, subdued cover details (for
                // example, greens amid Led Zeppelin IV's pale yellows).
                prefetchPlayerArtworkPalette(artworkModel, bitmap)
                locallyLoadedPalette = PlayerArtworkMemoryCache.getPalette(artworkModel)
            }
        }
    }
    val artworkRequestSize = with(LocalDensity.current) {
        artworkMaxSize.roundToPx().coerceAtLeast(1)
    }

    if (
        artworkModel != null &&
        (artwork == null || palette == null) &&
        !LocalInspectionMode.current
    ) {
        LandscapistImage(
            imageModel = { artworkModel },
            modifier = Modifier.size(0.dp),
            requestBuilder = { size(artworkRequestSize, artworkRequestSize) },
            success = { state, _ ->
                val loadedArtwork = remember(state.data) {
                    state.data?.let { convertToImageBitmap(it) }
                }
                LaunchedEffect(artworkModel, loadedArtwork) {
                    if (loadedArtwork != null) {
                        artwork = loadedArtwork
                        PlayerArtworkMemoryCache.putArtwork(artworkModel, loadedArtwork)
                    }
                }
            },
            failure = { state ->
                println("PLAYER ARTWORK FAILED: ${state.reason?.stackTraceToString()}")
            },
        )
    }

    val isDark = isSystemInDarkTheme()
    val scheme = remember(palette, isDark, artworkModel) {
        palette.artworkColorScheme(isDark, artworkModel?.hashCode() ?: 0)
    }
    CompositionLocalProvider(LocalPlayerArtwork provides artwork) {
        MaterialExpressiveTheme(animateColorScheme(scheme)) {
            Box(Modifier.playerBackground(coloredBackground)) { content() }
        }
    }
}

const val collapsedHorizontalPadding = 8
internal val playerBottomBarHeight = 64.dp
internal val playerBottomBarControlSize = 48.dp
internal const val PlayerBottomBarContentType = "player-bottom-bar"
internal const val PlayerScrollbarThumbSizePercent = 0.16f
internal const val PlayerArtworkCrossfadeDurationMs = 250

@Stable
internal class PlayerTimelineState(val durationMs: Float) {
    var positionMs by mutableFloatStateOf(0f)
    var isSeeking by mutableStateOf(false)
}

@Composable
internal fun rememberPlayerTimelineState(
    key: Any?,
    durationMs: Float,
    running: Boolean,
    onFinished: suspend () -> Boolean = { false },
): PlayerTimelineState {
    val state = remember(key, durationMs) { PlayerTimelineState(durationMs) }
    val currentOnFinished by rememberUpdatedState(onFinished)
    val isSeeking = state.isSeeking

    LaunchedEffect(state, running, isSeeking) {
        if (!running || isSeeking || state.positionMs >= state.durationMs) {
            return@LaunchedEffect
        }

        var previousFrameNanos = withFrameNanos { it }
        while (isActive) {
            val frameNanos = withFrameNanos { it }
            val elapsedMs = (frameNanos - previousFrameNanos) / 1_000_000f
            state.positionMs =
                (state.positionMs + elapsedMs).coerceAtMost(state.durationMs)
            if (state.positionMs >= state.durationMs) {
                state.positionMs = 0f
                if (currentOnFinished()) return@LaunchedEffect
            }
            previousFrameNanos = frameNanos
        }
    }

    return state
}

internal val LocalPlayerTimelineState = staticCompositionLocalOf<PlayerTimelineState?> { null }
internal val LocalPlayerArtwork = staticCompositionLocalOf<ImageBitmap?> { null }

private val CoverBrightnessFilter = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
    1.5f, 0f, 0f, 0f, 0f,
    0f, 1.5f, 0f, 0f, 0f,
    0f, 0f, 1.5f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)))

/** Brighten the actual cover pixels inside a one-pixel ring. */
internal fun Modifier.brightenedCoverOutline(
    outlineWidthPx: Float = 1f,
    outlineRadius: Dp = 8.dp,
): Modifier = drawWithContent {
    drawContent()

    val strokeWidth = outlineWidthPx.coerceAtLeast(0f)
    if (size.width <= strokeWidth * 2 || size.height <= strokeWidth * 2) return@drawWithContent
    val radius = outlineRadius.toPx().coerceAtLeast(0f)
    val ring = Path().apply {
        fillType = PathFillType.EvenOdd
        addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(radius)))
        addRoundRect(
            RoundRect(
                Rect(
                    strokeWidth,
                    strokeWidth,
                    size.width - strokeWidth,
                    size.height - strokeWidth,
                ),
                CornerRadius((radius - strokeWidth).coerceAtLeast(0f)),
            )
        )
    }

    drawContext.canvas.saveLayer(
        Rect(Offset.Zero, size),
        Paint().apply { colorFilter = CoverBrightnessFilter },
    )
    val redrawArtwork = { drawContent() }
    clipPath(ring) { redrawArtwork() }
    drawContext.canvas.restore()
}

@Composable
internal fun PlayerArtwork(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    outlineWidth: Dp? = null,
    outlineRadius: Dp = 4.dp,
) {
    val outlineWidthPx = outlineWidth?.let { with(LocalDensity.current) { it.toPx() } } ?: 1f
    Box(modifier.brightenedCoverOutline(outlineWidthPx, outlineRadius)) {
        Crossfade(
            targetState = LocalPlayerArtwork.current,
            modifier = Modifier.matchParentSize(),
            animationSpec = tween(PlayerArtworkCrossfadeDurationMs),
        ) { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                )
            } else {
                Box(Modifier.fillMaxSize())
            }
        }
    }
}

/** Page-local buttons stay in place during skips: select the target instantly,
 * then animate the new page's visibility instead of sliding its click targets. */
@Stable
internal class PlayerSkipNavigator(
    private val pagerState: PagerState,
    private val controls: PlayerControlsState,
    private val scope: CoroutineScope,
) {
    private var pendingSong by mutableStateOf<Int?>(null)
    private var navigationJob: Job? = null
    private var requestId = 0
    private val entryAnimation = Animatable(1f)
    var enteringSong by mutableStateOf<Int?>(null)
        private set
    val entryProgress: Float get() = entryAnimation.value

    val isNavigating: Boolean get() = pendingSong != null
    private fun originSong(): Int = pendingSong ?: controls.songAt(pagerState.targetPage)
    val canGoNext: Boolean get() = controls.nextSong(originSong()) != null

    fun next() = navigate(controls.nextSong(originSong()))
    fun previous() = navigate(controls.previousSong(originSong()))

    private fun navigate(song: Int?) {
        val destination = song?.let(controls::pageOf) ?: return
        if (destination !in 0 until pagerState.pageCount) return
        val id = ++requestId
        val willChangePage = pagerState.currentPage != destination
        pendingSong = song
        controls.currentSong = song
        enteringSong = if (willChangePage) song else null
        navigationJob?.cancel()
        navigationJob = scope.launch {
            try {
                if (willChangePage) {
                    entryAnimation.snapTo(0f)
                }
                pagerState.scrollToPage(destination)
                if (willChangePage) {
                    entryAnimation.animateTo(
                        1f,
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                    )
                }
            } finally {
                if (requestId == id) {
                    pendingSong = null
                    enteringSong = null
                    navigationJob = null
                }
            }
        }
    }

    /** Selecting from the queue takes priority over any skip in progress. */
    fun selectSong(song: Int) {
        val page = controls.pageOf(song)
        if (page < 0) return
        requestId++
        pendingSong = null
        enteringSong = null
        navigationJob?.cancel()
        navigationJob = null
        controls.currentSong = song
        pagerState.requestScrollToPage(page)
    }
}

/** Only fades and blurs pixels, never translates the page's pointer targets.
 * Manual horizontal swipes still use the normal pager slide transition. */
internal fun Modifier.animatedSkipEntrance(
    navigator: PlayerSkipNavigator?,
    song: Int,
): Modifier = graphicsLayer {
    if (navigator?.enteringSong == song) {
        val remaining = (1f - navigator.entryProgress).coerceIn(0f, 1f)
        alpha = 1f - remaining * 0.28f
        val blur = 18.dp.toPx() * remaining
        renderEffect = if (blur > 0.5f) BlurEffect(blur, blur, TileMode.Decal) else null
    } else {
        alpha = 1f
        renderEffect = null
    }
}

@Stable
internal class PlayerScrollbarItemSizes {
    private var sizes = IntArray(16)
    private var knownCount = 0
    private var knownTotal = 0L

    private fun ensureCapacity(index: Int) {
        if (index < sizes.size) return
        var newSize = sizes.size
        while (newSize <= index) newSize *= 2
        sizes = sizes.copyOf(newSize)
    }

    fun update(index: Int, size: Int) {
        if (index < 0 || size <= 0) return
        ensureCapacity(index)
        val previous = sizes[index]
        if (previous == size) return
        if (previous == 0) knownCount++ else knownTotal -= previous.toLong()
        sizes[index] = size
        knownTotal += size.toLong()
    }

    fun itemSize(index: Int, fallbackSize: Int): Int =
        sizes.getOrNull(index)?.takeIf { it > 0 } ?: fallbackSize

    fun fallbackSize(viewportSize: Int): Int = if (knownCount > 0) {
        (knownTotal.toDouble() / knownCount).roundToInt().coerceAtLeast(1)
    } else {
        viewportSize.coerceAtLeast(1)
    }

    fun estimatedContentHeight(totalItems: Int, fallbackSize: Int): Int {
        var total = 0L
        for (index in 0 until totalItems) {
            total += itemSize(index, fallbackSize).toLong()
        }
        return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun estimatedScrollOffset(
        firstVisibleIndex: Int,
        firstVisibleItemScrollOffset: Int,
        fallbackSize: Int,
    ): Int {
        var beforeFirstItem = 0L
        for (index in 0 until firstVisibleIndex) {
            beforeFirstItem += itemSize(index, fallbackSize).toLong()
        }
        val firstItemSize = itemSize(firstVisibleIndex, fallbackSize)
        val offset = firstVisibleItemScrollOffset.coerceIn(0, firstItemSize)
        return (beforeFirstItem + offset).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun itemAtOffset(
        targetScrollOffset: Int,
        totalItems: Int,
        fallbackSize: Int,
    ): Long {
        var remainingOffset = targetScrollOffset
        var targetIndex = 0
        while (targetIndex < totalItems - 1) {
            val itemSize = itemSize(targetIndex, fallbackSize)
            if (remainingOffset < itemSize) break
            remainingOffset -= itemSize
            targetIndex++
        }
        return (targetIndex.toLong() shl 32) or
                (remainingOffset.toLong() and 0xffffffffL)
    }
}

@Composable
internal fun rememberPlayerScrollbarState(
    listState: LazyListState,
    itemSizes: PlayerScrollbarItemSizes,
): ScrollbarState {
    val state = remember { ScrollbarState() }
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            var visibleItemsSignature = 1
            layoutInfo.visibleItemsInfo.forEach { item ->
                visibleItemsSignature = 31 * visibleItemsSignature + item.index
                visibleItemsSignature = 31 * visibleItemsSignature + item.size
            }
            PlayerScrollbarSnapshot(
                firstVisibleIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                canScrollBackward = listState.canScrollBackward,
                totalItems = layoutInfo.totalItemsCount,
                viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset,
                visibleItemsSignature = visibleItemsSignature,
            )
        }.collect { snapshot ->
            if (snapshot.totalItems <= 0) return@collect
            listState.layoutInfo.visibleItemsInfo.forEach { item ->
                itemSizes.update(item.index, item.size)
            }

            val viewportSize = snapshot.viewportSize.coerceAtLeast(1)
            val fallbackSize = itemSizes.fallbackSize(viewportSize)
            val totalHeight = itemSizes.estimatedContentHeight(
                totalItems = snapshot.totalItems,
                fallbackSize = fallbackSize,
            )
            val maxScrollOffset = (totalHeight - viewportSize).coerceAtLeast(1)
            val scrollOffset = itemSizes.estimatedScrollOffset(
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
    val visibleItemsSignature: Int,
)

@Composable
internal fun rememberPlayerScrollbarThumbMover(
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
        val totalHeight = itemSizes.estimatedContentHeight(totalItems, fallbackSize)
        val maxScrollOffset = (totalHeight - viewportSize).coerceAtLeast(0)
        val maxThumbTravel = 1f - PlayerScrollbarThumbSizePercent
        val targetScrollOffset = (maxScrollOffset *
                (thumbMovedPercent / maxThumbTravel).coerceIn(0f, 1f))
            .roundToInt()
        val target = itemSizes.itemAtOffset(
            targetScrollOffset = targetScrollOffset,
            totalItems = totalItems,
            fallbackSize = fallbackSize,
        )
        val targetIndex = (target ushr 32).toInt()
        val remainingOffset = target.toInt()
        listState.requestScrollToItem(targetIndex, remainingOffset)
    }
    return remember {
        { newPercentage -> thumbMovedPercent = newPercentage }
    }
}
