package dev.brahmkshatriya.echo

import androidx.compose.ui.window.ComposeUIViewController
import dev.brahmkshatriya.echo.app.playback.createPlaybackComponent
import dev.brahmkshatriya.echo.app.ui.App
import platform.UIKit.UIViewController

/** Retained by the SwiftUI application, independently of its view controllers. */
class EchoApplication : AutoCloseable {
    internal val playback = createPlaybackComponent()

    override fun close() = playback.close()
}

@Suppress("unused", "FunctionName")
fun MainViewController(application: EchoApplication): UIViewController =
    ComposeUIViewController { App(application.playback) }
