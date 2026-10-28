package dev.brahmkshatriya.echo

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.ComposeViewport
import dev.brahmkshatriya.echo.app.playback.createPlaybackComponent
import dev.brahmkshatriya.echo.app.ui.App

fun main() {
    val playback = createPlaybackComponent()
    ComposeViewport {
        DisposableEffect(playback) { onDispose { playback.close() } }
        App(playback)
    }
}
