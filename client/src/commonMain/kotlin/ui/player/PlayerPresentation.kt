package dev.brahmkshatriya.echo.app.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import dev.brahmkshatriya.echo.app.platform.KeepScreenAwake
import dev.brahmkshatriya.echo.app.ui.player.artwork.PreloadAdjacentArtwork
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.LocalLyricsBloomStrength
import dev.brahmkshatriya.echo.app.ui.player.song.lyrics.rememberLyricsBloomStrength

@Composable
internal fun ProvidePlayerPresentation(content: @Composable () -> Unit) {
    val pagerState = LocalPlayerPagerState.current
    val controls = LocalPlayerControls.current
    val presentation = LocalPlayerPresentation.current
    if (pagerState != null) PreloadAdjacentArtwork(pagerState, controls)
    val bloomStrength = rememberLyricsBloomStrength()
    KeepScreenAwake(
        enabled = (presentation.mode != PlayerPresentationMode.Cover ||
            controls.currentMedia?.kind == dev.brahmkshatriya.echo.app.playback.MediaKind.Video && controls.isPlaying) &&
            LocalPlayerSheet.current?.isExpandedState?.value == true,
    )
    CompositionLocalProvider(LocalLyricsBloomStrength provides bloomStrength, content = content)
}
