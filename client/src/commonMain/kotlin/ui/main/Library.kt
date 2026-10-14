package dev.brahmkshatriya.echo.app.ui.main

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.animateColorScheme
import dev.brahmkshatriya.echo.app.ui.player.LocalNowPlayingControls
import dev.brahmkshatriya.echo.app.ui.player.song.LocalPlayerItems
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSheet
import dev.brahmkshatriya.echo.app.ui.player.song.KineticShapesBackground
import dev.brahmkshatriya.echo.app.ui.player.song.LocalPlayerFavouriteAccent
import dev.brahmkshatriya.echo.app.ui.player.song.artworkColorScheme
import dev.brahmkshatriya.echo.app.ui.player.song.cachedPlayerArtworkPalette
import dev.brahmkshatriya.echo.app.ui.player.song.favouriteArtworkAccent

/** Full-page physics playground, using the same rounded page frame as Test(). */
@Composable
fun Library() {
    val playerExpanded = LocalPlayerSheet.current?.isExpandedState?.value == true
    val currentSong = LocalNowPlayingControls.current?.currentSong ?: 0
    val artwork = LocalPlayerItems.current.getOrNull(currentSong)
    val palette = artwork?.let(::cachedPlayerArtworkPalette)
    val seed = artwork?.hashCode() ?: 0
    val dark = isSystemInDarkTheme()
    val scheme = remember(palette, dark, seed) {
        palette.artworkColorScheme(dark, seed)
    }
    val favouriteAccent = remember(palette, seed) { palette.favouriteArtworkAccent(seed) }

    CompositionLocalProvider(LocalPlayerFavouriteAccent provides favouriteAccent) {
        MaterialExpressiveTheme(animateColorScheme(scheme)) {
            Box(
                Modifier.fillMaxSize()
                    .safeDrawingPadding()
                    .padding(8.dp)
                    .clip(shapes.large),
            ) {
                KineticShapesBackground(artworkKey = artwork.orEmpty(), active = !playerExpanded)
            }
        }
    }
}
