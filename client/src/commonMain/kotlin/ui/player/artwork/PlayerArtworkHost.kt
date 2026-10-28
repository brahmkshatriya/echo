package dev.brahmkshatriya.echo.app.ui.player.artwork

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kmpalette.generatePalette
import com.kmpalette.palette.graphics.Palette
import com.materialkolor.ktx.animateColorScheme
import com.skydoves.landscapist.core.ImageRequest
import com.skydoves.landscapist.core.model.ImageResult
import com.skydoves.landscapist.core.scheduler.DecodePriority
import com.skydoves.landscapist.image.LandscapistImage
import com.skydoves.landscapist.image.convertToImageBitmap
import com.skydoves.landscapist.image.getLandscapist
import dev.brahmkshatriya.echo.app.playback.PlayerSession
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerControls
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSheet
import dev.brahmkshatriya.echo.app.ui.player.PlayerItemHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun PreloadAdjacentArtwork(pagerState: PagerState, controls: PlayerSession) {
    val landscapist = getLandscapist()
    val requestSize = with(LocalDensity.current) {
        MaxPlayerArtworkSize.dp.roundToPx().coerceAtLeast(1)
    }

    LaunchedEffect(pagerState, controls, landscapist, requestSize) {
        snapshotFlow { pagerState.currentPage to controls.playbackOrder }.collectLatest { (currentPage, order) ->
            val adjacentPages = listOf(currentPage - 1, currentPage + 1)
                .mapNotNull { order.getOrNull(it) }


            coroutineScope {
                adjacentPages.forEach { page ->
                    launch {
                        val artworkModel = controls.mediaFor(page)?.artworkUri ?: return@launch
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

internal const val MaxPlayerArtworkSize = 360

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
internal fun PlayerArtworkHost(
    index: Int,
    artworkMaxSize: Dp,
    coloredBackground: Boolean = true,
    content: @Composable () -> Unit,
) {
    val artworkModel = LocalPlayerControls.current.mediaFor(index)?.artworkUri
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
        palette.artworkColorScheme(isDark, artworkModel.hashCode())
    }
    val favouriteAccent = remember(palette, artworkModel) {
        palette.favouriteArtworkAccent(artworkModel.hashCode())
    }
    val sheet = LocalPlayerSheet.current
    val animateArtworkColors by remember(sheet) {
        derivedStateOf { sheet == null || sheet.progressState.value > 0f }
    }
    // A cold artwork load should not recompose all retained, hidden controls
    // for every intermediate palette color while the compact player is shown.
    val displayedScheme = if (animateArtworkColors) animateColorScheme(scheme) else scheme
    CompositionLocalProvider(
        LocalPlayerArtwork provides artwork,
        LocalPlayerFavouriteAccent provides favouriteAccent,
    ) {
        MaterialExpressiveTheme(displayedScheme) {
            PlayerItemHost(
                backgroundColor = if (coloredBackground) colorScheme.primaryContainer else null,
                outlineColor = if (coloredBackground) colorScheme.primary.copy(alpha = 0.1f) else null,
            ) { content() }
        }
    }
}

internal const val PlayerArtworkCrossfadeDurationMs = 250

internal val LocalPlayerArtwork = staticCompositionLocalOf<ImageBitmap?> { null }
internal val LocalPlayerFavouriteAccent = staticCompositionLocalOf { Color(0xFFE47796) }

/** Draw the inset outline as a single stroke; cache its geometry between redraws. */
internal fun Modifier.grayCoverOutline(
    outlineColor: Color,
    outlineWidth: Dp = 0.5.dp,
    outlineRadius: Dp = 8.dp,
    outlineScale: () -> Float = { 1f },
): Modifier = drawWithCache {
    val baseStrokeWidth = outlineWidth.toPx().coerceAtLeast(0f)
    val baseRadius = outlineRadius.toPx()
    val color = outlineColor.copy(alpha = 0.5f)
    onDrawWithContent {
        drawContent()
        // Sheet scaling changes drawing, without recomposing the image subtree.
        val scale = outlineScale().coerceAtLeast(0.001f)
        val strokeWidth = baseStrokeWidth / scale
        if (strokeWidth > 0f && size.width > strokeWidth * 2 && size.height > strokeWidth * 2) {
            val inset = strokeWidth / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(inset, inset),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                cornerRadius = CornerRadius((baseRadius / scale - inset).coerceAtLeast(0f)),
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

@Composable
internal fun PlayerArtwork(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    outlineWidth: Dp = 0.5.dp,
    outlineRadius: Dp = 4.dp,
    outlineScale: () -> Float = { 1f },
) {
    Box(modifier.grayCoverOutline(Color.Gray, outlineWidth, outlineRadius, outlineScale)) {
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
