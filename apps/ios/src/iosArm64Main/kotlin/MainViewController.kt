package dev.brahmkshatriya.echo

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.skydoves.landscapist.image.LocalLandscapist
import dev.brahmkshatriya.echo.app.playback.createPlaybackComponent
import dev.brahmkshatriya.echo.app.ui.IosApp
import dev.brahmkshatriya.echo.app.ui.LocalPlaybackComponent
import platform.UIKit.UIViewController

/** Retained by the SwiftUI application, independently of its view controllers. */
class EchoApplication : AutoCloseable {
    val playback = createPlaybackComponent()

    override fun close() = playback.close()
}

@Suppress("unused", "FunctionName")
fun MainViewController(application: EchoApplication): UIViewController = ComposeUIViewController {
    CompositionLocalProvider(
        LocalPlaybackComponent provides application.playback,
        LocalLandscapist provides application.playback.imageLoader,
    ) {
        IosApp()
    }
}
