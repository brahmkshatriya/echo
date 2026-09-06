package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.onPointerScrollY
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
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
import dev.brahmkshatriya.echo.app.platform.VariableText
import dev.brahmkshatriya.echo.app.platform.hasTouchInput
import dev.brahmkshatriya.echo.app.ui.Media
import dev.brahmkshatriya.echo.app.ui.components.BetterImage
import dev.brahmkshatriya.echo.app.ui.components.BetterSheet
import dev.brahmkshatriya.echo.app.ui.components.FastScrollbar
import dev.brahmkshatriya.echo.app.ui.components.LocalMainBackStack
import dev.brahmkshatriya.echo.app.ui.components.ResponsiveRow
import dev.brahmkshatriya.echo.app.ui.components.ScaledTopAppBar
import dev.brahmkshatriya.echo.app.ui.components.ScrollbarState
import dev.brahmkshatriya.echo.app.ui.components.SquigglySlider
import dev.brahmkshatriya.echo.app.ui.components.materialGroup
import dev.brahmkshatriya.echo.app.ui.components.paddingMask
import dev.brahmkshatriya.echo.app.ui.components.scrollbarStateValue
import dev.brahmkshatriya.echo.app.ui.main.Header
import dev.brahmkshatriya.echo.app.ui.theme.Primary
import dev.brahmkshatriya.echo.app.ui.theme.googleSansFontFamily
import echo.app.generated.resources.Res
import echo.app.generated.resources.ic_back
import echo.app.generated.resources.ic_check_circle
import echo.app.generated.resources.ic_close
import echo.app.generated.resources.ic_close_small
import echo.app.generated.resources.ic_favorite
import echo.app.generated.resources.ic_favorite_filled
import echo.app.generated.resources.ic_keyboard_arrow_down
import echo.app.generated.resources.ic_keyboard_arrow_up
import echo.app.generated.resources.ic_lyrics_mic
import echo.app.generated.resources.ic_lyrics_mic_off
import echo.app.generated.resources.ic_more_vert
import echo.app.generated.resources.ic_pause
import echo.app.generated.resources.ic_pause_32
import echo.app.generated.resources.ic_play_arrow
import echo.app.generated.resources.ic_play_arrow_32
import echo.app.generated.resources.ic_playlist_remove
import echo.app.generated.resources.ic_repeat
import echo.app.generated.resources.ic_search_outline
import echo.app.generated.resources.ic_shuffle
import echo.app.generated.resources.ic_skip_next
import echo.app.generated.resources.ic_skip_next_32
import echo.app.generated.resources.ic_skip_previous
import echo.app.generated.resources.ic_skip_previous_32
import echo.app.generated.resources.ic_volume_off
import echo.app.generated.resources.ic_volume_up
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

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
    onLyricsSelectorOpenChanged: (Boolean) -> Unit = {},
) {
    val artworkModel = LocalPlayerItems.current.getOrNull(i)
    val cachedArtwork = remember(artworkModel) {
        artworkModel?.let(PlayerArtworkMemoryCache::get)
    }
    var artwork by remember(artworkModel) { mutableStateOf(cachedArtwork?.artwork) }
    var palette by remember(artworkModel) { mutableStateOf(cachedArtwork?.palette) }
    val artworkRequestSize = with(LocalDensity.current) {
        maxSongCoverSize.dp.roundToPx().coerceAtLeast(1)
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
            Box(Modifier.playerBackground(true)) {
                SongPlayerItem(
                    i = i,
                    onScrolledToTopChanged = onScrolledToTopChanged,
                    onLyricsSelectorOpenChanged = onLyricsSelectorOpenChanged,
                )
            }
        }
    }
}

const val maxSongCoverSize = 360
const val songCoverHorizontalPadding = 16
const val songCoverVerticalPadding = 16
const val collapsedHorizontalPadding = 8
private val playerBottomBarHeight = 64.dp
private val playerBottomBarInset = 8.dp
private val playerBottomBarControlSize = 48.dp
private val playerBottomBarItemSpacing = 4.dp
private const val PlayerBottomBarContentType = "player-bottom-bar"
private const val PlayerQueueItemContentType = "player-queue-item"
private const val PlayerQueueItemsPerGroup = 11
private const val PlayerScrollbarThumbSizePercent = 0.16f
private const val LyricsWaitingGapMs = 1_000L
private const val LyricsWaitingDotsHoldMs = 3_000L
private const val LyricsTransitionDurationMs = 240
private const val LyricsModeTransitionDurationMs = 320
private const val PlayerArtworkCrossfadeDurationMs = 250

private class LyricsSelectorRevealShape(
    private val sourceBounds: Rect?,
    private val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val source = sourceBounds ?: Rect(0f, 0f, size.width, size.height)
        val resolvedProgress = progress.coerceIn(0f, 1f)

        fun interpolate(start: Float, end: Float): Float =
            start + (end - start) * resolvedProgress

        val left = interpolate(source.left, 0f)
        val top = interpolate(source.top, 0f)
        val right = interpolate(source.right, size.width)
        val bottom = interpolate(source.bottom, size.height)
        val radius = with(density) {
            playerBottomBarControlSize.toPx() / 2f * (1f - resolvedProgress)
        }

        return Outline.Rounded(
            RoundRect(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                topLeftCornerRadius = CornerRadius(radius),
                topRightCornerRadius = CornerRadius(radius),
                bottomLeftCornerRadius = CornerRadius(radius),
                bottomRightCornerRadius = CornerRadius(radius),
            )
        )
    }
}

private fun Modifier.lyricsSelectorTransition(
    sourceBounds: Rect?,
    progress: Float,
    containerColor: Color,
): Modifier {
    val resolvedProgress = progress.coerceIn(0f, 1f)
    return clip(
        LyricsSelectorRevealShape(sourceBounds, resolvedProgress)
    ).background(containerColor)
}

private data class LyricsSelectionItem(
    val title: String,
    val subtitle: String,
    val extension: String,
    val lyrics: Lyrics,
)

private val demoLyricsSelections = listOf(
    LyricsSelectionItem(
        title = "Synced lyrics with an intentionally very long title for collapsed app bar testing",
        subtitle = "Spotify • Word synced lyrics",
        extension = "Spotify",
        lyrics = niceLyrics,
    ),
    LyricsSelectionItem(
        title = "Synced lyrics",
        subtitle = "LRC Lib • Line synced lyrics",
        extension = "LRC Lib",
        lyrics = lineLyricsExample,
    ),
    LyricsSelectionItem(
        title = "Rich synced lyrics",
        subtitle = "MusixMatch • Word synced with translations",
        extension = "MusixMatch",
        lyrics = selfLoveLyrics,
    ),
    LyricsSelectionItem(
        title = "Plain lyrics",
        subtitle = "Genius • Unsynced lyrics",
        extension = "Genius",
        lyrics = plainLyricsExample,
    ),
    LyricsSelectionItem(
        title = "Synced lyrics",
        subtitle = "YouTube Music • Word synced lyrics",
        extension = "YouTube Music",
        lyrics = niceLyrics,
    ),
    LyricsSelectionItem(
        title = "Alternative synced lyrics",
        subtitle = "YouTube • Line synced lyrics",
        extension = "YouTube",
        lyrics = lineLyricsExample,
    ),
    LyricsSelectionItem(
        title = "Translated lyrics",
        subtitle = "Apple Music • Word synced with translations",
        extension = "Apple Music",
        lyrics = selfLoveLyrics,
    ),
    LyricsSelectionItem(
        title = "Community synced lyrics",
        subtitle = "NetEase • Line synced lyrics",
        extension = "NetEase",
        lyrics = lineLyricsExample,
    ),
    LyricsSelectionItem(
        title = "Fast synced lyrics",
        subtitle = "QQ Music • Word synced lyrics",
        extension = "QQ Music",
        lyrics = rapGodLyrics,
    ),
    LyricsSelectionItem(
        title = "Alternative rich lyrics",
        subtitle = "MusixMatch • Word synced lyrics",
        extension = "MusixMatch",
        lyrics = niceLyrics,
    ),
    LyricsSelectionItem(
        title = "Alternative plain lyrics",
        subtitle = "Genius • Unsynced lyrics",
        extension = "Genius",
        lyrics = plainLyricsExample,
    ),
    LyricsSelectionItem(
        title = "Community lyrics",
        subtitle = "LRC Lib • Unsynced lyrics",
        extension = "LRC Lib",
        lyrics = plainLyricsExample,
    ),
)

private object LyricsSelectorPlayerInfo : NavigationEventInfo()
private object LyricsSelectorPageInfo : NavigationEventInfo()

private val LyricsPosition.textAlign: TextAlign
    get() = when (this) {
        LyricsPosition.Start -> TextAlign.Start
        LyricsPosition.End -> TextAlign.End
    }

private val LyricsPosition.contentAlignment: Alignment
    get() = when (this) {
        LyricsPosition.Start -> Alignment.CenterStart
        LyricsPosition.End -> Alignment.CenterEnd
    }

private fun upperBound(values: LongArray, value: Long): Int {
    var low = 0
    var high = values.size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (values[mid] <= value) low = mid + 1 else high = mid
    }
    return low
}

private fun lowerBound(values: LongArray, value: Long): Int {
    var low = 0
    var high = values.size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (values[mid] < value) low = mid + 1 else high = mid
    }
    return low
}

private sealed interface FullLyricsLineTiming {
    val line: TimedLyricsLine
    val text: String
}

private class WordLineTiming(override val line: WordsLyric) : FullLyricsLineTiming {
    override val text = buildString {
        line.tokens.forEach { token ->
            append(token.text)
            append(token.trailingSpace)
        }
    }
    val tokenOffsets = IntArray(line.tokens.size)
    private val tokenStarts = LongArray(line.tokens.size)
    private val tokenLengths = IntArray(line.tokens.size)
    private val lastPosition = (text.length - 1).coerceAtLeast(0).toFloat()

    init {
        var offset = 0
        line.tokens.forEachIndexed { index, token ->
            tokenOffsets[index] = offset
            tokenStarts[index] = token.startMs
            tokenLengths[index] = (token.text.length + token.trailingSpace.length).coerceAtLeast(1)
            offset += token.text.length + token.trailingSpace.length
        }
    }

    fun tokenIndexAt(positionMs: Long): Int = upperBound(tokenStarts, positionMs) - 1

    fun peakPositionAt(positionMs: Long): Float {
        if (line.tokens.isEmpty()) return 0f
        val tokenIndex = tokenIndexAt(positionMs)
        val token = line.tokens.getOrNull(tokenIndex) ?: return 0f
        val tokenProgress = if (token.endMs > token.startMs) {
            ((positionMs - token.startMs).toFloat() /
                    (token.endMs - token.startMs).toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }
        val tokenPeakRange = (tokenLengths[tokenIndex] - 1).coerceAtLeast(0).toFloat()
        return (tokenOffsets[tokenIndex] + tokenPeakRange * tokenProgress)
            .coerceIn(0f, lastPosition)
    }
}

private class LineTiming(override val line: LineLyric) : FullLyricsLineTiming {
    override val text: String = line.text
}

private class LyricsTimingIndex private constructor(val lines: List<FullLyricsLineTiming>) {
    private val lineStarts = LongArray(lines.size) { lines[it].line.startMs }
    private val lineEnds = LongArray(lines.size) { lines[it].line.endMs }

    companion object {
        fun word(lines: List<WordsLyric>) = LyricsTimingIndex(lines.map(::WordLineTiming))

        fun line(lines: List<LineLyric>) = LyricsTimingIndex(lines.map(::LineTiming))
    }

    fun lineIndexAtOrBefore(positionMs: Long): Int = upperBound(lineStarts, positionMs) - 1

    fun currentLineIndex(positionMs: Long): Int = lineIndexAtOrBefore(positionMs).coerceAtLeast(0)

    fun activeLineIndex(positionMs: Long): Int {
        val index = lineIndexAtOrBefore(positionMs)
        val line = lines.getOrNull(index)?.line ?: return -1
        return if (positionMs in line.startMs..line.endMs) index else -1
    }

    fun hasWaitingGapAfter(lineIndex: Int): Boolean {
        val nextLine = lines.getOrNull(lineIndex + 1)?.line ?: return false
        val previousEndMs = lines.getOrNull(lineIndex)?.line?.endMs ?: 0L
        return nextLine.startMs - previousEndMs >= LyricsWaitingGapMs
    }

    fun activeWaitingGapAfterIndex(positionMs: Long): Int? {
        val previousIndex = lineIndexAtOrBefore(positionMs)
        val previousLine = lines.getOrNull(previousIndex)?.line
        if (previousLine != null && positionMs <= previousLine.endMs) return null

        val nextLine = lines.getOrNull(previousIndex + 1)?.line ?: return null
        return previousIndex.takeIf {
            hasWaitingGapAfter(previousIndex) && positionMs < nextLine.startMs
        }
    }

    fun visibleWaitingGapAfterIndex(positionMs: Long): Int? {
        activeWaitingGapAfterIndex(positionMs)?.let { return it }

        var nextLineIndex = lineIndexAtOrBefore(positionMs)
        while (nextLineIndex >= 0) {
            val nextLine = lines[nextLineIndex].line
            if (positionMs - nextLine.startMs >= LyricsWaitingDotsHoldMs) return null

            val gapIndex = nextLineIndex - 1
            if (hasWaitingGapAfter(gapIndex)) return gapIndex
            nextLineIndex--
        }
        return null
    }

    fun latestCompletedLineIndex(positionMs: Long): Int = lowerBound(lineEnds, positionMs) - 1
}

@Stable
private class PlayerTimelineState(val durationMs: Float) {
    var positionMs by mutableFloatStateOf(0f)
    var isSeeking by mutableStateOf(false)
}

private val LocalPlayerTimelineState = staticCompositionLocalOf<PlayerTimelineState?> { null }
private val LocalPlayerArtwork = staticCompositionLocalOf<ImageBitmap?> { null }

@Composable
private fun PlayerArtwork(
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
private fun rememberPlayerScrollbarState(
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
    onLyricsSelectorOpenChanged: (Boolean) -> Unit = {},
) = CompositionLocalProvider(
    LocalContentColor provides colorScheme.onPrimaryContainer
) {
    val timelineState = remember(i) {
        PlayerTimelineState(niceLyrics.lines.lastOrNull()?.endMs?.toFloat() ?: 0f)
    }
    val controls = LocalPlayerControls.current
    val pagerState = LocalPlayerPagerState.current
    val isCurrentItem = pagerState == null || pagerState.currentPage == i
    val isPlaying = controls?.isPlaying != false
    val isSeeking = timelineState.isSeeking
    LaunchedEffect(timelineState, isCurrentItem, isPlaying, isSeeking, controls, pagerState) {
        if (
            !isCurrentItem ||
            !isPlaying ||
            isSeeking ||
            timelineState.positionMs >= timelineState.durationMs
        ) return@LaunchedEffect

        var previousFrameNanos = withFrameNanos { it }
        while (isActive) {
            val frameNanos = withFrameNanos { it }
            val elapsedMs = (frameNanos - previousFrameNanos) / 1_000_000f
            timelineState.positionMs =
                (timelineState.positionMs + elapsedMs).coerceAtMost(timelineState.durationMs)
            if (timelineState.positionMs >= timelineState.durationMs) {
                timelineState.positionMs = 0f
                if (controls?.repeatEnabled != true) {
                    if (pagerState != null) {
                        pagerState.playNext(controls?.shuffleEnabled == true)
                        return@LaunchedEffect
                    }
                }
            }
            previousFrameNanos = frameNanos
        }
    }
    var playerBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    CompositionLocalProvider(LocalPlayerTimelineState provides timelineState) {
        BoxWithConstraints(
            Modifier.onGloballyPositioned { playerBoundsInRoot = it.boundsInRoot() }
        ) {
            val playerSheet = LocalPlayerSheet.current
            val scope = rememberCoroutineScope()
            val backStack = LocalMainBackStack.current
            val cardColors = CardDefaults.cardColors(
                containerColor = colorScheme.surface,
                contentColor = colorScheme.onSurface
            )
            val listState = rememberLazyListState()
            var fallbackShowLyrics by remember { mutableStateOf(false) }
            val sharedLyricsVisible = LocalPlayerLyricsVisible.current
            val showLyrics = sharedLyricsVisible?.value ?: fallbackShowLyrics
            var selectedLyricsIndex by remember(i) { mutableStateOf<Int?>(0) }
            val selectedLyrics = selectedLyricsIndex?.let { index ->
                demoLyricsSelections.getOrNull(index)
            }
            var showLyricsSelector by remember(i) { mutableStateOf(false) }
            var lyricsPillBoundsInRoot by remember(i) { mutableStateOf<Rect?>(null) }
            var selectorRevealSourceBounds by remember(i) { mutableStateOf<Rect?>(null) }
            val selectorRevealProgress = remember(i) { Animatable(0f) }
            var selectorMounted by remember(i) { mutableStateOf(false) }
            val selectorRevealAnimationSpec = motionScheme.defaultSpatialSpec<Float>()

            val selectorNavigationEventState = rememberNavigationEventState(
                currentInfo = if (showLyricsSelector) {
                    LyricsSelectorPageInfo
                } else {
                    LyricsSelectorPlayerInfo
                },
                backInfo = if (showLyricsSelector) {
                    listOf(LyricsSelectorPlayerInfo)
                } else {
                    emptyList()
                },
            )
            if (LocalNavigationEventDispatcherOwner.current != null) {
                NavigationBackHandler(
                    state = selectorNavigationEventState,
                    isBackEnabled = showLyricsSelector,
                    onBackCompleted = { showLyricsSelector = false },
                    onBackCancelled = {},
                )
            }
            LaunchedEffect(showLyricsSelector, selectorNavigationEventState) {
                if (!showLyricsSelector) {
                    if (selectorMounted) {
                        selectorRevealProgress.animateTo(
                            targetValue = 0f,
                            animationSpec = selectorRevealAnimationSpec,
                        )
                        selectorMounted = false
                    }
                } else {
                    selectorMounted = true
                    snapshotFlow {
                        when (val state = selectorNavigationEventState.transitionState) {
                            NavigationEventTransitionState.Idle -> false to 1f
                            is NavigationEventTransitionState.InProgress ->
                                true to (1f - state.latestEvent.progress).coerceIn(0f, 1f)
                        }
                    }.collectLatest { (isBackGesture, progress) ->
                        if (isBackGesture) {
                            selectorRevealProgress.snapTo(progress)
                        } else {
                            selectorRevealProgress.animateTo(
                                targetValue = progress,
                                animationSpec = selectorRevealAnimationSpec,
                            )
                        }
                    }
                }
            }
            LaunchedEffect(selectorMounted) {
                onLyricsSelectorOpenChanged(selectorMounted)
            }
            LaunchedEffect(showLyrics) {
                if (!showLyrics) showLyricsSelector = false
            }
            val isPlayerScrolledToTop by remember {
                derivedStateOf { !listState.canScrollBackward }
            }
            val layoutDirection = LocalLayoutDirection.current
            val safeDrawing = WindowInsets.safeDrawing.asPaddingValues()
            val topPadding = safeDrawing.calculateTopPadding()
            val bottomPadding = safeDrawing.calculateBottomPadding()
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
            val playerViewportHeightPx = constraints.maxHeight
            val transformModifier = Modifier.expandedListItemTransform(playerSheet) {
                playerViewportHeightPx
            }
            val heroHeight = (maxHeight - topPadding - bottomPadding - playerBottomBarHeight)
                .coerceAtLeast(0.dp)
            val coverViewportWidth = maxWidth
            LazyColumn(
                Modifier.paddingMask(safeDrawing) {
                    playerSheet?.progressState?.floatValue ?: 0f
                },
                state = listState,
                contentPadding = PaddingValues(
                    top = topPadding,
                    start = safeDrawing.calculateStartPadding(layoutDirection),
                    end = safeDrawing.calculateEndPadding(layoutDirection),
                )
            ) {
                item(key = "player-hero-$i") {
                    PlayerHero(
                        i = i,
                        lyrics = selectedLyrics?.lyrics,
                        showLyrics = showLyrics,
                        userScrollEnabled = isPlayerScrolledToTop,
                        topPadding = topPadding,
                        viewportWidth = coverViewportWidth,
                        transformModifier = transformModifier,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(heroHeight),
                        onArtworkClick = {
                            if (sharedLyricsVisible != null) {
                                sharedLyricsVisible.value = false
                            } else {
                                fallbackShowLyrics = false
                            }
                        },
                    )
                }
                stickyHeader(bottomBarKey, PlayerBottomBarContentType, isSlidable = false) {
                    Box(transformModifier.height(playerBottomBarHeight)) {
                        BottomBar(
                            index = i,
                            isSticky = isBottomBarSticky,
                            lyricsVisible = showLyrics,
                            selectedLyrics = selectedLyrics,
                            lyricsSelectorActive = selectorMounted,
                            onLyricsPillBoundsChanged = { lyricsPillBoundsInRoot = it },
                            onLyricsSelectorClick = {
                                selectorRevealSourceBounds =
                                    lyricsPillBoundsInRoot?.let { pillBounds ->
                                        playerBoundsInRoot?.let { playerBounds ->
                                            Rect(
                                                left = pillBounds.left - playerBounds.left,
                                                top = pillBounds.top - playerBounds.top,
                                                right = pillBounds.right - playerBounds.left,
                                                bottom = pillBounds.bottom - playerBounds.top,
                                            )
                                        }
                                    }
                                showLyricsSelector = true
                            },
                            onLyricsClick = {
                                listState.requestScrollToItem(0)
                                if (sharedLyricsVisible != null) {
                                    sharedLyricsVisible.value = !sharedLyricsVisible.value
                                } else {
                                    fallbackShowLyrics = !fallbackShowLyrics
                                }
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

            CollapsedPlayer(i = i, showCover = showLyrics)
            val scrollbarItemSizes = remember { PlayerScrollbarItemSizes() }
            val scrollbarState = rememberPlayerScrollbarState(listState, scrollbarItemSizes)
            FastScrollbar(
                modifier = transformModifier
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
            if (selectorMounted) {
                val progress = selectorRevealProgress.value.coerceIn(0f, 1f)
                val sourceBounds = selectorRevealSourceBounds
                val density = LocalDensity.current
                val pillContentAlpha = (1f - progress * 2f).coerceIn(0f, 1f)
                val pageContentAlpha = ((progress - 0.5f) * 2f).coerceIn(0f, 1f)
                val containerColor = lerp(
                    colorScheme.primary.copy(alpha = 0.1f),
                    colorScheme.surfaceContainer,
                    progress,
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .lyricsSelectorTransition(
                            sourceBounds = sourceBounds,
                            progress = progress,
                            containerColor = containerColor,
                        )
                ) {
                    sourceBounds?.let { bounds ->
                        LyricsSelectionPillContent(
                            selectedLyrics = selectedLyrics,
                            modifier = Modifier
                                .size(
                                    width = with(density) { bounds.width.toDp() },
                                    height = with(density) { bounds.height.toDp() },
                                )
                                .graphicsLayer {
                                    translationX = bounds.left
                                    translationY = bounds.top
                                    alpha = pillContentAlpha
                                },
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = pageContentAlpha }
                    ) {
                        LyricsSelectionPage(
                            selectedLyrics = selectedLyrics,
                            items = demoLyricsSelections,
                            topPadding = topPadding,
                            bottomPadding = bottomPadding,
                            onSelect = { selection ->
                                val index = demoLyricsSelections.indexOf(selection)
                                if (index >= 0) {
                                    selectedLyricsIndex = index
                                    showLyricsSelector = false
                                }
                            },
                            onClearSelection = { selectedLyricsIndex = null },
                            onBack = { showLyricsSelector = false },
                        )
                    }
                }
            }
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
        alpha = if (positiveProgress > 0.5f) (positiveProgress - 0.5f) * 2 else 0f
        translationY = offset * playerHeight() * 0.33f
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
    lyricsVisible: Boolean = false,
    onArtworkClick: () -> Unit = {},
) {
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
    val mergedStyle = LocalTextStyle.current.merge(typography.labelLarge)

    SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->
        val looseConstraints = constraints.copy(minHeight = 0)
        val metadata = subcompose("metadata") {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(lyricsVisible) {
                    PlayerArtwork(
                        contentDescription = "Song $i artwork",
                        modifier = Modifier
                            .padding(start = 16.dp, end = 8.dp)
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorScheme.primaryFixed)
                            .clickable(onClick = onArtworkClick),
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
        }.single().measure(looseConstraints)

        val times = subcompose("times") {
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
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(interactionSource = endInteraction) {
                            showRemainingTime = !showRemainingTime
                        }
                        .padding(8.dp),
                )
            }
        }.single().measure(looseConstraints)

        val sliderHorizontalPadding = 16.dp.roundToPx()
        val sliderWidth = (constraints.maxWidth - sliderHorizontalPadding * 2).coerceAtLeast(0)
        val slider = subcompose("slider") {
            PlayerSlider(
                modifier = Modifier.fillMaxWidth().height(72.dp),
                timelineState = timelineState,
            )
        }.single().measure(
            Constraints(
                minWidth = sliderWidth,
                maxWidth = sliderWidth,
                minHeight = 0,
                maxHeight = looseConstraints.maxHeight,
            )
        )

        val formatBadge = subcompose("format") {
            Text(
                text = "FLAC",
                style = mergedStyle,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(100))
                    .background(colorScheme.primary.copy(0.1f))
                    .clickable {}
                    .padding(12.dp, 4.dp),
            )
        }.single().measure(looseConstraints.copy(minWidth = 0))

        val metadataToTimesSpacing = 40.dp.roundToPx()
        val sliderTop = (metadata.height - 12.dp.roundToPx()).coerceAtLeast(0)
        val contentHeight = maxOf(
            metadata.height + metadataToTimesSpacing + times.height,
            sliderTop + slider.height,
            formatBadge.height,
        ).coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(constraints.maxWidth, contentHeight) {
            slider.placeRelative(sliderHorizontalPadding, sliderTop)
            metadata.placeRelative(0, 0)
            times.placeRelative(0, metadata.height + metadataToTimesSpacing)
            formatBadge.placeRelative(
                x = (constraints.maxWidth - formatBadge.width) / 2,
                y = contentHeight - formatBadge.height,
            )
        }
    }
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

private enum class PlayerHeroSlot {
    TopBar,
    CoverOrLyrics,
    Timeline,
    Controller,
}

@Composable
private fun PlayerHero(
    i: Int,
    lyrics: Lyrics?,
    showLyrics: Boolean,
    userScrollEnabled: Boolean,
    topPadding: Dp,
    viewportWidth: Dp,
    transformModifier: Modifier,
    onArtworkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(modifier) { constraints ->
        val childConstraints = constraints.copy(minHeight = 0)
        val topBar = subcompose(PlayerHeroSlot.TopBar) {
            TopBar(i, transformModifier)
        }.single().measure(childConstraints)
        val timeline = subcompose(PlayerHeroSlot.Timeline) {
            Box(transformModifier) {
                ExpandedTimeline(
                    i = i,
                    lyricsVisible = showLyrics,
                    onArtworkClick = onArtworkClick,
                )
            }
        }.single().measure(childConstraints)
        val controller = subcompose(PlayerHeroSlot.Controller) {
            Box(transformModifier) { Controller() }
        }.single().measure(childConstraints)

        val coverHeight = (
                constraints.maxHeight - topBar.height - timeline.height - controller.height
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
                            userScrollEnabled = userScrollEnabled,
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
            .coverSize(maxCoverSize, verticalPadding)
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
private fun LyricsPanel(
    lyrics: Lyrics,
    userScrollEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val timelineState = LocalPlayerTimelineState.current ?: return
    val isPlaying = LocalPlayerControls.current?.isPlaying != false
    val contentModifier = modifier.padding(horizontal = 12.dp)
    when (lyrics) {
        is Lyrics.Simple -> BoxWithConstraints(
            modifier = contentModifier.fillMaxSize(),
        ) {
            val verticalContentPadding = if (constraints.hasBoundedHeight) maxHeight / 2 else 64.dp
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .lyricsEdgeFade(),
                userScrollEnabled = userScrollEnabled,
                contentPadding = PaddingValues(vertical = verticalContentPadding),
            ) {
                item {
                    Text(
                        text = lyrics.text,
                        modifier = Modifier.fillMaxWidth(),
                        style = typography.headlineMedium,
                        color = colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        is Lyrics.Line -> FullTimedLyrics(
            timingIndex = remember(lyrics.lines) { LyricsTimingIndex.line(lyrics.lines) },
            timelineState = timelineState,
            isPlaying = isPlaying,
            userScrollEnabled = userScrollEnabled,
            modifier = contentModifier,
        )

        is Lyrics.Word -> FullTimedLyrics(
            timingIndex = remember(lyrics.lines) { LyricsTimingIndex.word(lyrics.lines) },
            timelineState = timelineState,
            isPlaying = isPlaying,
            userScrollEnabled = userScrollEnabled,
            modifier = contentModifier,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FullTimedLyrics(
    timingIndex: LyricsTimingIndex,
    timelineState: PlayerTimelineState,
    isPlaying: Boolean,
    userScrollEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val syncResumeJob = remember { mutableListOf<Job?>(null) }
    var autoScrollSuppressed by remember(timingIndex) { mutableStateOf(false) }
    var hasInitialLyricsPosition by remember(timingIndex) { mutableStateOf(false) }
    val currentLineIndex by remember(timingIndex, timelineState) {
        derivedStateOf { timingIndex.currentLineIndex(timelineState.positionMs.toLong()) }
    }
    val activeLineIndex by remember(timingIndex, timelineState) {
        derivedStateOf { timingIndex.activeLineIndex(timelineState.positionMs.toLong()) }
    }
    val latestCompletedLineIndex by remember(timingIndex, timelineState) {
        derivedStateOf { timingIndex.latestCompletedLineIndex(timelineState.positionMs.toLong()) }
    }
    val activeWaitingGapAfterIndex by remember(timingIndex, timelineState) {
        derivedStateOf {
            timingIndex.activeWaitingGapAfterIndex(timelineState.positionMs.toLong())
        }
    }
    val visibleWaitingGapAfterIndex by remember(timingIndex, timelineState) {
        derivedStateOf {
            timingIndex.visibleWaitingGapAfterIndex(timelineState.positionMs.toLong())
        }
    }
    val isSeeking = timelineState.isSeeking
    val syncArrowPointsUp by remember(listState, currentLineIndex) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val currentItem = layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == currentLineIndex }
            if (currentItem != null) {
                val viewportCenter =
                    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                currentItem.offset + currentItem.size / 2 < viewportCenter
            } else {
                currentLineIndex < listState.firstVisibleItemIndex
            }
        }
    }
    val manualScrollConnection = remember(userScrollEnabled, scope) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (
                    userScrollEnabled &&
                    source == NestedScrollSource.UserInput &&
                    available.y != 0f
                ) {
                    autoScrollSuppressed = true
                    syncResumeJob[0]?.cancel()
                    syncResumeJob[0] = scope.launch {
                        delay(3_000.milliseconds)
                        if (!currentIsPlaying) {
                            snapshotFlow { currentIsPlaying }.first { it }
                        }
                        autoScrollSuppressed = false
                    }
                }
                return Offset.Zero
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val lyricsViewportHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else 0
        val verticalContentPadding = if (constraints.hasBoundedHeight) maxHeight / 2 else 64.dp
        val waitingDotsHalfHeightPx = with(LocalDensity.current) { 22.dp.roundToPx() }

        LaunchedEffect(
            currentLineIndex,
            activeWaitingGapAfterIndex,
            timingIndex.lines,
            lyricsViewportHeight,
            autoScrollSuppressed,
            isPlaying,
            isSeeking,
        ) {
            if (
                (autoScrollSuppressed && !isSeeking) ||
                (!isPlaying && !isSeeking) ||
                timingIndex.lines.isEmpty() ||
                lyricsViewportHeight == 0
            ) return@LaunchedEffect

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
            val centerOffset = when (activeWaitingGapAfterIndex) {
                -1 -> waitingDotsHalfHeightPx
                currentLineIndex -> targetItem.size + waitingDotsHalfHeightPx
                else -> targetItem.size / 2
            }
            val shouldAnimate = hasInitialLyricsPosition && targetWasVisible
            if (shouldAnimate) listState.animateScrollToItem(currentLineIndex, centerOffset)
            else listState.requestScrollToItem(currentLineIndex, centerOffset)
            hasInitialLyricsPosition = true
        }

        if (timingIndex.lines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                LyricsWaitingDots(
                    animated = false,
                    modifier = Modifier.size(width = 32.dp, height = 32.dp),
                )
            }
        } else {
            LazyColumn(
                state = listState,
                userScrollEnabled = userScrollEnabled,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(manualScrollConnection)
                    .lyricsEdgeFade(),
                contentPadding = PaddingValues(vertical = verticalContentPadding),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(
                    items = timingIndex.lines,
                    key = { index, timing -> "${timing.line.startMs}-$index" }
                ) { lineIndex, lineTiming ->
                    val startsNewPositionGroup = lineIndex > 0 &&
                            timingIndex.lines[lineIndex - 1].line.position != lineTiming.line.position
                    Column(
                        modifier = Modifier.padding(
                            top = if (startsNewPositionGroup) 14.dp else 0.dp
                        )
                    ) {
                        AnimatedFullLyricsWaitingDots(
                            visible = lineIndex == 0 && visibleWaitingGapAfterIndex == -1,
                            animated = isPlaying && activeWaitingGapAfterIndex == -1,
                            position = lineTiming.line.position,
                        )
                        val onLineClick = {
                            timelineState.positionMs = lineTiming.line.startMs
                                .toFloat()
                                .coerceIn(0f, timelineState.durationMs)
                        }
                        when (lineTiming) {
                            is WordLineTiming -> FullTimedLyricsLine(
                                lineTiming = lineTiming,
                                timelineState = timelineState,
                                isActive = lineIndex == activeLineIndex,
                                isPast = lineIndex <= latestCompletedLineIndex,
                                waitingGapEndMs = timingIndex.lines.getOrNull(lineIndex + 1)
                                    ?.line?.startMs
                                    ?.takeIf { timingIndex.hasWaitingGapAfter(lineIndex) },
                                retainsEndTrail = lineIndex == latestCompletedLineIndex &&
                                        activeWaitingGapAfterIndex != lineIndex,
                                trailTiming = (timingIndex.lines.getOrNull(currentLineIndex)
                                        as? WordLineTiming)
                                    ?.takeIf { currentLineIndex > lineIndex },
                                onClick = onLineClick,
                            )

                            is LineTiming -> FullLineLyricsLine(
                                lineTiming = lineTiming,
                                isActive = lineIndex == activeLineIndex,
                                isPast = lineIndex <= latestCompletedLineIndex,
                                onClick = onLineClick,
                            )
                        }
                        AnimatedFullLyricsWaitingDots(
                            visible = visibleWaitingGapAfterIndex == lineIndex,
                            animated = isPlaying && activeWaitingGapAfterIndex == lineIndex,
                            position = lineTiming.line.position,
                        )
                    }
                }
            }
        }

        val syncArrowRotation by animateFloatAsState(
            targetValue = if (syncArrowPointsUp) 180f else 0f,
            animationSpec = tween(220, easing = FastOutSlowInEasing),
        )

        AnimatedVisibility(
            visible = autoScrollSuppressed,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Button(
                onClick = {
                    syncResumeJob[0]?.cancel()
                    syncResumeJob[0] = null
                    autoScrollSuppressed = false
                    if (!isPlaying) {
                        scope.launch {
                            val targetWasVisible = listState.layoutInfo.visibleItemsInfo
                                .any { it.index == currentLineIndex }
                            if (!targetWasVisible) {
                                listState.requestScrollToItem(currentLineIndex)
                            }
                            val targetItem = snapshotFlow {
                                listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.index == currentLineIndex }
                            }.first { it != null } ?: return@launch
                            withFrameNanos { }
                            listState.animateScrollToItem(
                                currentLineIndex,
                                targetItem.size / 2,
                            )
                            hasInitialLyricsPosition = true
                        }
                    }
                },
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 10.dp,
                    end = 8.dp,
                    bottom = 10.dp
                ),
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.onSecondaryContainer,
                    contentColor = colorScheme.secondaryContainer,
                ),
            ) {
                Text("Sync")
                Spacer(Modifier.width(4.dp))
                Icon(
                    painter = painterResource(Res.drawable.ic_keyboard_arrow_down),
                    contentDescription = if (syncArrowPointsUp) "Sync up" else "Sync down",
                    modifier = Modifier.graphicsLayer {
                        rotationZ = syncArrowRotation
                    },
                )
            }
        }
    }
}

@Composable
private fun AnimatedFullLyricsWaitingDots(
    visible: Boolean,
    animated: Boolean,
    position: LyricsPosition,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = tween(LyricsTransitionDurationMs, easing = FastOutSlowInEasing),
        ) + fadeIn(tween(LyricsTransitionDurationMs)),
        exit = shrinkVertically(
            animationSpec = tween(LyricsTransitionDurationMs, easing = FastOutSlowInEasing),
        ) + fadeOut(tween(LyricsTransitionDurationMs)),
    ) {
        FullLyricsWaitingDots(animated = animated, position = position)
    }
}

@Composable
private fun FullLyricsWaitingDots(
    animated: Boolean,
    position: LyricsPosition,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = position.contentAlignment,
    ) {
        LyricsWaitingDots(animated, Modifier.size(width = 32.dp, height = 32.dp))
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
    lineTiming: WordLineTiming,
    timelineState: PlayerTimelineState,
    isActive: Boolean,
    isPast: Boolean,
    waitingGapEndMs: Long?,
    retainsEndTrail: Boolean,
    trailTiming: WordLineTiming?,
    onClick: () -> Unit,
) {
    val line = lineTiming.line
    val lineAlpha by animateFloatAsState(
        targetValue = if (isActive || isPast) 1f else 0.32f,
        animationSpec = tween(360, easing = FastOutSlowInEasing)
    )
    val lyricColor = colorScheme.onPrimaryContainer
    val interactionSource = remember(line) { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val interactionAlpha by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.12f
            isHovered -> 0.08f
            else -> 0f
        },
        animationSpec = tween(120)
    )
    val lineText = lineTiming.text
    val textAlign = line.position.textAlign

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                lyricColor.copy(alpha = interactionAlpha),
                RoundedCornerShape(8.dp)
            ).graphicsLayer {
                alpha = lineAlpha
            }.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if ((isActive || isPast) && lineText.isNotEmpty()) {
            val peakPosition = remember(
                lineTiming,
                timelineState,
                isActive,
                isPast,
                waitingGapEndMs,
                retainsEndTrail,
                trailTiming,
                lineText,
            ) {
                when {
                    isPast && waitingGapEndMs != null -> {
                        {
                            val positionMs = timelineState.positionMs.toLong()
                            val gapDurationMs = (waitingGapEndMs - line.endMs).coerceAtLeast(1L)
                            val gapProgress = (
                                    (positionMs - line.endMs).toFloat() / gapDurationMs.toFloat()
                                    ).coerceIn(0f, 1f)
                            lineText.lastIndex.toFloat() + lineText.length * gapProgress
                        }
                    }

                    retainsEndTrail && trailTiming != null -> {
                        {
                            val positionMs = timelineState.positionMs.toLong()
                            val trailOffset = trailTiming.line
                                .takeIf { positionMs in it.startMs..it.endMs }
                                ?.let { trailTiming.peakPositionAt(positionMs) }
                                ?: 0f
                            lineText.lastIndex.toFloat() + trailOffset
                        }
                    }

                    retainsEndTrail -> {
                        { lineText.lastIndex.toFloat() }
                    }

                    isActive -> {
                        { lineTiming.peakPositionAt(timelineState.positionMs.toLong()) }
                    }

                    else -> {
                        { null }
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val density = LocalDensity.current
                val maxWidthPx = with(density) { maxWidth.roundToPx() }
                val referenceFontFamily = googleSansFontFamily()
                val textMeasurer = rememberTextMeasurer()
                val referenceStyle = typography.headlineMedium.copy(
                    fontFamily = referenceFontFamily,
                    fontWeight = FontWeight.Normal,
                    lineHeight = typography.headlineLarge.lineHeight,
                    textAlign = textAlign,
                )
                val referenceLayout = remember(
                    lineText,
                    maxWidthPx,
                    referenceStyle,
                    textMeasurer,
                ) {
                    textMeasurer.measure(
                        text = lineText,
                        style = referenceStyle,
                        softWrap = true,
                        maxLines = Int.MAX_VALUE,
                        constraints = Constraints(
                            minWidth = maxWidthPx,
                            maxWidth = maxWidthPx,
                        ),
                    )
                }
                val glyphXPositions = remember(lineText, referenceLayout) {
                    FloatArray(lineText.length) { index ->
                        referenceLayout.getHorizontalPosition(
                            index,
                            usePrimaryDirection = true,
                        )
                    }
                }
                val glyphBaselines = remember(lineText, referenceLayout) {
                    FloatArray(lineText.length) { index ->
                        referenceLayout.getLineBaseline(
                            referenceLayout.getLineForOffset(index)
                        )
                    }
                }
                val textHeight = with(density) { referenceLayout.size.height.toDp() }

                VariableText(
                    text = lineText,
                    peakPosition = peakPosition,
                    glyphXPositions = glyphXPositions,
                    glyphBaselines = glyphBaselines,
                    color = lyricColor,
                    fontSize = typography.headlineMedium.fontSize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(textHeight),
                )
            }
        } else {
            Text(
                text = lineText,
                modifier = Modifier.fillMaxWidth(),
                style = typography.headlineMedium,
                lineHeight = typography.headlineLarge.lineHeight,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
            )
        }

        line.backgroundVocals.takeIf { it.isNotEmpty() }?.let { vocals ->
            Text(
                text = vocals.joinToString("") { it.text + it.trailingSpace },
                modifier = Modifier.fillMaxWidth(),
                style = typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
                color = lyricColor.copy(alpha = 0.72f)
            )
        }
        line.translations.forEach { translation ->
            Text(
                text = translation.text,
                modifier = Modifier.fillMaxWidth(),
                style = typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
                color = lyricColor.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun FullLineLyricsLine(
    lineTiming: LineTiming,
    isActive: Boolean,
    isPast: Boolean,
    onClick: () -> Unit,
) {
    val line = lineTiming.line
    val textAlign = line.position.textAlign
    val lineAlpha by animateFloatAsState(
        targetValue = if (isActive || isPast) 1f else 0.32f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
    )
    val lyricColor = colorScheme.onPrimaryContainer
    val interactionSource = remember(line) { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val interactionAlpha by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.12f
            isHovered -> 0.08f
            else -> 0f
        },
        animationSpec = tween(120),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                lyricColor.copy(alpha = interactionAlpha),
                RoundedCornerShape(8.dp),
            )
            .graphicsLayer { alpha = lineAlpha }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = lineTiming.text,
            modifier = Modifier.fillMaxWidth(),
            style = typography.headlineMedium,
            lineHeight = typography.headlineLarge.lineHeight,
            fontWeight = FontWeight.Normal,
            textAlign = textAlign,
        )
        line.translations.forEach { translation ->
            Text(
                text = translation.text,
                modifier = Modifier.fillMaxWidth(),
                style = typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
                color = lyricColor.copy(alpha = 0.78f),
            )
        }
    }
}


@Composable
private fun BottomBar(
    index: Int = 0,
    isSticky: Boolean = false,
    lyricsVisible: Boolean = false,
    selectedLyrics: LyricsSelectionItem? = demoLyricsSelections.first(),
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
                .background(colorScheme.primary.copy(0.1f))
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
private fun LyricsSelectionPillContent(
    selectedLyrics: LyricsSelectionItem?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (selectedLyrics != null) {
            Text(
                text = selectedLyrics.title,
                style = typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = selectedLyrics.extension,
                style = typography.labelSmall,
                color = colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = "Choose Lyrics",
                style = typography.labelLarge,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsSelectionPage(
    selectedLyrics: LyricsSelectionItem?,
    items: List<LyricsSelectionItem>,
    topPadding: Dp,
    bottomPadding: Dp,
    onSelect: (LyricsSelectionItem) -> Unit,
    onClearSelection: () -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    val filteredItems = remember(items, appliedQuery) {
        val needle = appliedQuery.trim()
        if (needle.isEmpty()) {
            items
        } else {
            items.filter { item ->
                item.title.contains(needle, ignoreCase = true) ||
                        item.subtitle.contains(needle, ignoreCase = true) ||
                        item.extension.contains(needle, ignoreCase = true)
            }
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val resultsListState = rememberLazyListState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent,
        contentColor = colorScheme.onSurface,
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            contentColor = colorScheme.onSurface,
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                ScaledTopAppBar(
                    modifier = Modifier.padding(top = topPadding),
                    title = selectedLyrics?.title ?: "No lyrics selected",
                    subtitle = selectedLyrics?.subtitle ?: "Choose a lyrics source",
                    actions = {
                        IconButton(
                            onClick = onClearSelection,
                            enabled = selectedLyrics != null,
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_playlist_remove),
                                contentDescription = "Deselect lyrics",
                            )
                        }
                    },
                    windowInsets = WindowInsets(0.dp),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                LyricsSelectionSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onClear = {
                        query = ""
                        appliedQuery = ""
                    },
                    onSearch = { appliedQuery = query.trim() },
                    onBack = onBack,
                    bottomPadding = bottomPadding,
                )
            },
        ) { contentPadding ->
            val cardColors = CardDefaults.cardColors(
                containerColor = colorScheme.surface
            )
            LazyColumn(
                state = resultsListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentPadding = PaddingValues(horizontal = 8.dp),
                reverseLayout = true,
                verticalArrangement = Arrangement.Bottom,
            ) {
                materialGroup(
                    lazyListState = resultsListState,
                    gap = 3.dp,
                    reverseLayout = true,
                ) {
                    filteredItems.forEach { item ->
                        card(
                            key = "${item.extension}:${item.title}:${item.subtitle}",
                            contentType = "lyrics-selection",
                            colors = cardColors
                        ) {
                            LyricsSelectionRow(
                                item = item,
                                selected = item == selectedLyrics,
                                onClick = { onSelect(item) },
                            )
                        }
                    }
                }
                if (filteredItems.isEmpty()) {
                    item {
                        Text(
                            text = "No lyrics found",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            style = typography.bodyLarge,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsSelectionRow(
    item: LyricsSelectionItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = colorScheme.onSurface,
            )
            Text(
                text = item.subtitle.substringAfter(" • "),
                style = typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                painter = painterResource(Res.drawable.ic_check_circle),
                contentDescription = "Selected lyrics",
                modifier = Modifier.size(24.dp),
                tint = colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LyricsSelectionSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    bottomPadding: Dp,
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = playerBottomBarInset,
                end = playerBottomBarInset,
                top = playerBottomBarInset,
                bottom = bottomPadding + playerBottomBarInset,
            ),
        horizontalArrangement = Arrangement.spacedBy(playerBottomBarItemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            shapes = IconButtonDefaults.shapes()
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_back),
                contentDescription = "Back",
            )
        }
        Row(
            modifier = Modifier
                .height(playerBottomBarControlSize)
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(colorScheme.primary.copy(alpha = 0.08f))
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search lyrics",
                        style = typography.bodyLarge,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) {
                                return@onPreviewKeyEvent false
                            }
                            when (event.key) {
                                Key.Escape -> {
                                    focusManager.clearFocus()
                                    true
                                }

                                Key.Enter -> {
                                    onSearch()
                                    true
                                }

                                else -> false
                            }
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    textStyle = typography.bodyLarge.copy(color = colorScheme.onSurface),
                    cursorBrush = Brush.verticalGradient(
                        listOf(colorScheme.primary, colorScheme.primary)
                    ),
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_close_small),
                        contentDescription = "Clear search",
                    )
                }
            }
            IconButton(
                onClick = onSearch,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_search_outline),
                    contentDescription = "Search lyrics",
                )
            }
        }
        IconButton(
            onClick = { },
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
private fun NoLyricsTicker() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Lyrics",
            style = typography.titleMedium,
            color = colorScheme.onPrimaryContainer,
            maxLines = 1,
        )
    }
}

private data class LyricsTickerContent(
    val lineIndex: Int?,
    val order: Int
)

private fun lyricsTickerContent(
    timingIndex: LyricsTimingIndex,
    positionMs: Long,
): LyricsTickerContent {
    val previousIndex = timingIndex.lineIndexAtOrBefore(positionMs)
    val previousLine = timingIndex.lines.getOrNull(previousIndex)?.line
    if (previousLine != null && positionMs <= previousLine.endMs) {
        return LyricsTickerContent(lineIndex = previousIndex, order = previousIndex * 2)
    }

    val nextIndex = previousIndex + 1
    val nextLine = timingIndex.lines.getOrNull(nextIndex)?.line
    val gapMs = if (previousLine == null) nextLine?.startMs ?: Long.MAX_VALUE
    else (nextLine?.startMs ?: Long.MAX_VALUE) - previousLine.endMs
    return if (nextLine != null && gapMs < LyricsWaitingGapMs)
        LyricsTickerContent(lineIndex = nextIndex, order = nextIndex * 2)
    else
        LyricsTickerContent(lineIndex = null, order = previousIndex * 2 + 1)
}

@Composable
private fun LyricsTicker(
    lyrics: Lyrics,
    timelineState: PlayerTimelineState,
) {
    when (lyrics) {
        is Lyrics.Word -> TimedLyricsTicker(lyrics, timelineState)
        is Lyrics.Line -> LineLyricsTicker(lyrics, timelineState)
        is Lyrics.Simple -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Lyrics",
                style = typography.titleMedium,
                color = colorScheme.onPrimaryContainer,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LineLyricsTicker(
    lyrics: Lyrics.Line,
    timelineState: PlayerTimelineState,
) {
    val timingIndex = remember(lyrics.lines) { LyricsTimingIndex.line(lyrics.lines) }
    val isPlaying = LocalPlayerControls.current?.isPlaying != false
    val content by remember(timingIndex, timelineState) {
        derivedStateOf {
            lyricsTickerContent(timingIndex, timelineState.positionMs.toLong())
        }
    }
    AnimatedContent(
        targetState = content,
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        transitionSpec = {
            val direction = if (targetState.order >= initialState.order) 1 else -1
            val animation = tween<IntOffset>(
                durationMillis = LyricsTransitionDurationMs,
                easing = FastOutSlowInEasing,
            )
            val enter = slideInVertically(animation) { direction * it } +
                    fadeIn(tween(LyricsTransitionDurationMs))
            val exit = slideOutVertically(animation) { -direction * it } +
                    fadeOut(tween(LyricsTransitionDurationMs))
            enter togetherWith exit
        },
    ) { target ->
        val lineTiming = target.lineIndex
            ?.let(timingIndex.lines::getOrNull) as? LineTiming
        if (lineTiming == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                LyricsWaitingDots(
                    animated = isPlaying,
                    modifier = Modifier.size(width = 32.dp, height = 32.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = lineTiming.text,
                    style = typography.titleMedium,
                    color = colorScheme.onPrimaryContainer,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TimedLyricsTicker(
    lyrics: Lyrics.Word,
    timelineState: PlayerTimelineState,
) {
    val timingIndex = remember(lyrics.lines) { LyricsTimingIndex.word(lyrics.lines) }
    val isPlaying = LocalPlayerControls.current?.isPlaying != false
    val content by remember(timingIndex, timelineState) {
        derivedStateOf {
            lyricsTickerContent(timingIndex, timelineState.positionMs.toLong())
        }
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
        val lineTiming = target.lineIndex
            ?.let(timingIndex.lines::getOrNull) as? WordLineTiming
        if (lineTiming == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                LyricsWaitingDots(
                    animated = isPlaying,
                    modifier = Modifier.size(width = 32.dp, height = 32.dp),
                )
            }
        } else TimedLyricsLine(lineTiming, timelineState)
    }
}

@Composable
private fun LyricsWaitingDots(
    animated: Boolean,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val phase = remember { Animatable(0f) }
    val motionStrength by animateFloatAsState(
        targetValue = if (animated) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "Lyrics waiting dots motion strength",
    )
    LaunchedEffect(Unit) {
        while (true) {
            val start = phase.value % 1f
            phase.snapTo(start)
            phase.animateTo(
                targetValue = start + 1f,
                animationSpec = tween(durationMillis = 900, easing = LinearEasing),
            )
        }
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val pulse = (
                    cos(phase.value * 2f * PI.toFloat() - index * 2f * PI.toFloat() / 3f) + 1f
                    ) / 2f
            val animatedScale = 0.55f + pulse * 0.45f
            val animatedAlpha = 0.55f + pulse * 0.4f
            val scale = 0.78f + (animatedScale - 0.78f) * motionStrength
            val alpha = 0.75f + (animatedAlpha - 0.75f) * motionStrength
            Box(
                Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scaleX
                    }
                    .clip(Circle.toShape())
                    .background(
                        colorScheme.onPrimaryContainer.copy(alpha = alpha)
                    )
            )
        }
    }
}

@Composable
private fun TimedLyricsLine(
    lineTiming: WordLineTiming,
    timelineState: PlayerTimelineState,
) {
    val line = lineTiming.line
    if (line.tokens.isEmpty()) return

    val lineText = lineTiming.text
    if (lineText.isEmpty()) return

    val activeTokenIndex by remember(lineTiming, timelineState) {
        derivedStateOf { lineTiming.tokenIndexAt(timelineState.positionMs.toLong()) }
    }
    val peakPosition = remember(lineTiming, timelineState) {
        { lineTiming.peakPositionAt(timelineState.positionMs.toLong()) }
    }

    val highlightedColor = colorScheme.onPrimaryContainer
    val referenceFontFamily = googleSansFontFamily()
    val textMeasurer = rememberTextMeasurer()
    val titleMediumStyle = typography.titleMedium
    val referenceStyle = remember(referenceFontFamily, titleMediumStyle) {
        titleMediumStyle.copy(
            fontFamily = referenceFontFamily,
            fontWeight = FontWeight.Normal,
        )
    }
    val marqueeLayout = remember(lineText, referenceStyle, textMeasurer) {
        textMeasurer.measure(
            text = lineText,
            style = referenceStyle,
            maxLines = 1,
            softWrap = false,
            constraints = Constraints(maxWidth = Constraints.Infinity),
        )
    }
    val glyphXPositions = remember(lineText, marqueeLayout) {
        FloatArray(lineText.length) { index ->
            marqueeLayout.getHorizontalPosition(index, usePrimaryDirection = true)
        }
    }
    val glyphBaselines = remember(lineText, marqueeLayout) {
        val baseline = marqueeLayout.getLineBaseline(0)
        FloatArray(lineText.length) { baseline }
    }
    val focusTokenIndex = activeTokenIndex.coerceAtLeast(0)
        .coerceAtMost(line.tokens.lastIndex)
    val targetFocusX = marqueeLayout.tokenCenter(lineTiming, focusTokenIndex)
    val focusX by animateFloatAsState(
        targetValue = targetFocusX,
        animationSpec = tween()
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewportWidth = if (constraints.hasBoundedWidth) {
            constraints.maxWidth.toFloat()
        } else {
            marqueeLayout.size.width.toFloat()
        }
        val viewportHeight = if (constraints.hasBoundedHeight) {
            constraints.maxHeight.toFloat()
        } else {
            marqueeLayout.size.height.toFloat()
        }
        val offsetX = if (marqueeLayout.size.width <= viewportWidth) {
            (viewportWidth - marqueeLayout.size.width) / 2f
        } else {
            viewportWidth / 2f - focusX
        }
        val offsetY = (viewportHeight - marqueeLayout.size.height) / 2f

        VariableText(
            text = lineText,
            peakPosition = peakPosition,
            glyphXPositions = glyphXPositions,
            glyphBaselines = glyphBaselines,
            offsetX = offsetX,
            offsetY = offsetY,
            color = highlightedColor,
            fontSize = typography.titleMedium.fontSize,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun TextLayoutResult.tokenCenter(
    lineTiming: WordLineTiming,
    tokenIndex: Int,
): Float {
    val token = lineTiming.line.tokens[tokenIndex]
    val startOffset = lineTiming.tokenOffsets[tokenIndex].coerceAtMost(layoutInput.text.lastIndex)
    val endOffset = (startOffset + token.text.length - 1)
        .coerceIn(startOffset, layoutInput.text.lastIndex)
    return (getBoundingBox(startOffset).left + getBoundingBox(endOffset).right) / 2f
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

        item(priority = 90, key = "play-pause") {
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

fun formatTime(ms: Float): String {
    val totalSeconds = (ms / 1000).toInt().coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Composable
fun TimelineWithVolume(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.widthIn(256.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Timeline(Modifier.weight(1f))
        VolumeAdjuster()
    }
}

@Composable
fun Timeline(modifier: Modifier = Modifier) {
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
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = currentTimeLabel,
            style = mergedStyle,
            modifier = Modifier.widthIn(min = 40.dp),
            textAlign = TextAlign.Center
        )

        PlayerSlider(
            modifier = Modifier
                .weight(1f)
                .height(32.dp),
            timelineState = timelineState,
        )

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
}

@Composable
private fun PlayerSlider(
    modifier: Modifier = Modifier,
    timelineState: PlayerTimelineState,
) {
    val rangeMS = remember(timelineState.durationMs) { 0f..timelineState.durationMs }
    val segmentGaps = remember(timelineState.durationMs) {
        listOf(0.22f, 0.48f, 0.72f, 0.88f).map { fraction ->
            timelineState.durationMs * fraction
        }
    }
    val controls = LocalPlayerControls.current
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isDragged) {
        timelineState.isSeeking = isDragged
    }

    SquigglySlider(
        valueRange = rangeMS,
        value = { timelineState.positionMs },
        onValueChange = { newValue ->
            timelineState.isSeeking = true
            timelineState.positionMs = newValue
        },
        onValueChangeFinished = {
            timelineState.isSeeking = false
        },
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        interactionSource = interactionSource,
        segmentGaps = segmentGaps,
        segmentGapWidth = 3.dp,
        squiggleAmplitude = if (controls?.isPlaying != false) 0.66f else 0f,
        squiggleWavelength = 40.dp,
        waveSpeed = 40.dp,
        trackHeight = 16.dp,
        trackStrokeWidth = 4.dp,
        draggedTrackStrokeWidth = 12.dp,
        trackCornerSize = Dp.Unspecified,
        trackInsideCornerSize = 2.dp,
        stopIndicatorSize = 3.dp,
        thumbSize = DpSize(4.dp, 32.dp),
        thumbTrackGap = 3.dp,
        activeColor = colorScheme.primary,
        inactiveColor = colorScheme.primary.copy(alpha = 0.25f),
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VolumeAdjuster() {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState()
    val hasTouch = hasTouchInput.value
    val position = remember { mutableFloatStateOf(1f) }
    val lastAudiblePosition = remember { mutableFloatStateOf(1f) }
    var isSliderPinned by remember { mutableStateOf(false) }
    val sliderInteraction = remember { MutableInteractionSource() }
    val isSliderDragged = sliderInteraction.collectIsDraggedAsState()

    fun setVolume(value: Float) {
        val volume = value.coerceIn(0f, 1f)
        position.floatValue = volume
        if (volume > 0f) lastAudiblePosition.floatValue = volume
    }

    fun toggleMute() {
        if (position.floatValue > 0f) {
            lastAudiblePosition.floatValue = position.floatValue
            position.floatValue = 0f
        } else {
            position.floatValue = lastAudiblePosition.floatValue.coerceAtLeast(0.01f)
        }
    }

    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(100))
            .hoverable(interactionSource)
            .onPointerScrollY { delta ->
                val newVolume = position.floatValue - delta * 0.05f
                setVolume(newVolume)
            }
            .background(colorScheme.primary.copy(0.25f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(isHovered.value || isSliderDragged.value || (hasTouch && isSliderPinned)) {
            SquigglySlider(
                value = { position.floatValue },
                modifier = Modifier
                    .width(96.dp)
                    .height(40.dp)
                    .padding(start = 12.dp, end = 4.dp)
                    .pointerHoverIcon(PointerIcon.Hand),
                interactionSource = sliderInteraction,
                onValueChange = ::setVolume,
                squiggleAmplitude = 0f,
                trackStrokeWidth = 4.dp,
                draggedTrackStrokeWidth = 8.dp,
                thumbSize = DpSize(4.dp, 28.dp),
                activeColor = colorScheme.primary,
                inactiveColor = colorScheme.primary.copy(0.25f),
            )
        }
        IconButton(
            onClick = {
                if (hasTouch) isSliderPinned = !isSliderPinned
                else toggleMute()
            },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painterResource(
                    if (position.floatValue == 0f) Res.drawable.ic_volume_off
                    else Res.drawable.ic_volume_up,
                ),
                contentDescription = if (hasTouch) {
                    if (isSliderPinned) "Unpin volume slider" else "Pin volume slider"
                } else {
                    if (position.floatValue == 0f) "Unmute" else "Mute"
                },
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
