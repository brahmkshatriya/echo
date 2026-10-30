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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.animateColorScheme
import dev.brahmkshatriya.echo.app.ui.LocalPlaybackComponent
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSheet
import dev.brahmkshatriya.echo.app.ui.player.artwork.LocalPlayerFavouriteAccent
import dev.brahmkshatriya.echo.app.ui.player.artwork.artworkColorScheme
import dev.brahmkshatriya.echo.app.ui.player.artwork.cachedPlayerArtworkPalette
import dev.brahmkshatriya.echo.app.ui.player.artwork.favouriteArtworkAccent
import dev.brahmkshatriya.echo.app.ui.player.song.KineticShapesBackground
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.MotionLyrics
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.rememberLyricsSelections
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.rememberMotionLyricsPosition

/** Full-page physics playground, using the same rounded page frame as Test(). */
@Composable
fun Library() {
    val session = LocalPlaybackComponent.current.session
    val playerExpanded = LocalPlayerSheet.current?.isExpandedState?.value == true
    val currentEntryId = session.currentEntryId
    val artwork = session.mediaFor(currentEntryId)?.artworkUri
    val palette = artwork?.let(::cachedPlayerArtworkPalette)
    val seed = artwork.hashCode()
    val dark = isSystemInDarkTheme()
    val scheme = remember(palette, dark, seed) {
        palette.artworkColorScheme(dark, seed)
    }
    val favouriteAccent = remember(palette, seed) { palette.favouriteArtworkAccent(seed) }
    val lyricsSelections = rememberLyricsSelections()
    val selectedIndex = dev.brahmkshatriya.echo.app.ui.player.LocalPlayerPresentation.current
        .lyricsSource(session.mediaFor(currentEntryId)?.id)
    val lyrics = selectedIndex?.let(lyricsSelections::getOrNull)?.lyrics
    val positionMs = rememberMotionLyricsPosition(session.currentMedia?.let { session.timelineFor(currentEntryId) }, active = !playerExpanded)

    CompositionLocalProvider(LocalPlayerFavouriteAccent provides favouriteAccent) {
        MaterialExpressiveTheme(animateColorScheme(scheme)) {
            Box(
                Modifier.fillMaxSize()
                    .safeDrawingPadding()
                    .padding(8.dp)
                    .clip(shapes.large),
            ) {
                KineticShapesBackground(bassLevel = session.bassLevel, artworkKey = artwork.orEmpty(), active = !playerExpanded)
                MotionLyrics(
                    lyrics = lyrics,
                    positionMs = positionMs,
                    modifier = Modifier.align(Alignment.Center).fillMaxSize().padding(24.dp),
                )
            }
        }
    }
}
