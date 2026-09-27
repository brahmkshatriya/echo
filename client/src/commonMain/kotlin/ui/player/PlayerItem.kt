package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.animation.Crossfade
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kmpalette.color
import com.kmpalette.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.rememberDynamicMaterialThemeState
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.image.LandscapistImage
import com.skydoves.landscapist.image.convertToImageBitmap
import com.skydoves.landscapist.palette.PalettePlugin
import dev.brahmkshatriya.echo.app.ui.components.ScrollbarState
import dev.brahmkshatriya.echo.app.ui.components.scrollbarStateValue
import dev.brahmkshatriya.echo.app.ui.player.song.SongPlayerItem
import dev.brahmkshatriya.echo.app.ui.player.song.maxSongCoverSize
import dev.brahmkshatriya.echo.app.ui.theme.Primary
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
import kotlin.random.Random

private data class PlayerArtworkCacheEntry(
    val artwork: ImageBitmap? = null,
    val palette: Palette? = null,
)

private object PlayerArtworkMemoryCache {
    private const val MAX_ENTRIES = 12
    private val entries = LinkedHashMap<String, PlayerArtworkCacheEntry>()

    fun get(model: String): PlayerArtworkCacheEntry? {
        val entry = entries.remove(model) ?: return null
        entries[model] = entry
        return entry
    }

    fun putArtwork(model: String, artwork: ImageBitmap) {
        put(model, (entries[model] ?: PlayerArtworkCacheEntry()).copy(artwork = artwork))
    }

    fun putPalette(model: String, palette: Palette) {
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
    var palette by remember(artworkModel) { mutableStateOf(cachedArtwork?.palette) }
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
            component = rememberImageComponent {
                add(
                    PalettePlugin(
                        imageModel = artworkModel,
                        paletteLoadedListener = { loadedPalette ->
                            palette = loadedPalette
                            PlayerArtworkMemoryCache.putPalette(artworkModel, loadedPalette)
                        },
                    )
                )
            },
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

    val color = palette?.let {
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
    CompositionLocalProvider(LocalPlayerArtwork provides artwork) {
        MaterialExpressiveTheme(animateColorScheme(scheme)) {
            Box(Modifier.playerBackground(coloredBackground)) { content() }
        }
    }
}

const val collapsedHorizontalPadding = 8
internal val playerBottomBarHeight = 64.dp
internal val playerBottomBarInset = 8.dp
internal val playerBottomBarControlSize = 48.dp
internal val playerBottomBarItemSpacing = 4.dp
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

@Composable
internal fun PlayerArtwork(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Crossfade(
        targetState = LocalPlayerArtwork.current,
        modifier = modifier,
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

internal suspend fun PagerState.playPrevious() {
    if (pageCount <= 0) return
    animateScrollToPage((currentPage - 1 + pageCount) % pageCount)
}

internal suspend fun PagerState.playNext(shuffle: Boolean) {
    if (pageCount <= 0) return
    val targetPage = if (shuffle && pageCount > 1)
        (currentPage + Random.nextInt(1, pageCount)) % pageCount
    else (currentPage + 1) % pageCount
    animateScrollToPage(targetPage)
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
