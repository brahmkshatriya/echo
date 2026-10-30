package dev.brahmkshatriya.echo

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.ComposeViewport
import com.skydoves.landscapist.image.LocalLandscapist
import dev.brahmkshatriya.echo.app.playback.createPlaybackComponent
import dev.brahmkshatriya.echo.app.ui.App
import dev.brahmkshatriya.echo.app.ui.LocalPlaybackComponent

fun main() {
    val playback = createPlaybackComponent()
    ComposeViewport {
        DisposableEffect(playback) { onDispose { playback.close() } }
        CompositionLocalProvider(
            LocalPlaybackComponent provides playback,
            LocalLandscapist provides playback.imageLoader,
        ) {
            App()
        }
    }
}
