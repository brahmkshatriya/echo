package dev.brahmkshatriya.echo.app.ui.player.song

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import dev.brahmkshatriya.echo.app.platform.KeepScreenAwake
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerSheet
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerPagerState
import dev.brahmkshatriya.echo.app.ui.player.LocalPlayerControls

val LocalPlayerLyricsVisible = staticCompositionLocalOf<MutableState<Boolean>?> { null }
val LocalPlayerKineticShapesVisible = staticCompositionLocalOf<MutableState<Boolean>?> { null }
val LocalPlayerLyricsChromeCollapsed = staticCompositionLocalOf<MutableState<Boolean>?> { null }

@Composable
internal fun ProvideSongPlayerState(content: @Composable () -> Unit) {
    val pagerState = LocalPlayerPagerState.current
    val controls = LocalPlayerControls.current
    if (pagerState != null && controls != null) {
        PreloadAdjacentSongArtwork(pagerState, controls)
    }
    val lyricsVisible = rememberSaveable { mutableStateOf(false) }
    val kineticShapesVisible = rememberSaveable { mutableStateOf(false) }
    val lyricsChromeCollapsed = rememberSaveable { mutableStateOf(false) }

    KeepScreenAwake(
        enabled = (lyricsVisible.value || kineticShapesVisible.value) &&
            LocalPlayerSheet.current?.isExpandedState?.value == true,
    )

    CompositionLocalProvider(
        LocalPlayerLyricsVisible provides lyricsVisible,
        LocalPlayerKineticShapesVisible provides kineticShapesVisible,
        LocalPlayerLyricsChromeCollapsed provides lyricsChromeCollapsed,
        content = content,
    )
}
