package dev.brahmkshatriya.echo.app.platform

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.Clipboard

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
@get:Composable
expect val hasTouchInput: State<Boolean>

/** Android exposes IME animation endpoints; other targets only expose IME insets. */
@Composable
expect fun imeAnimationInsetsOrNull(): Pair<WindowInsets, WindowInsets>?
/** Prevent screen timeout while the expanded player displays immersive content. */
@Composable
expect fun KeepScreenAwake(enabled: Boolean)

expect suspend fun Clipboard.setPlainText(text: String)

/**
 * Encodes as lossless PNG when [quality] is -1, or JPEG at a quality from 0 to 100.
 * Limits the longest edge to [maxSize], preserving aspect ratio without upscaling.
 */
expect fun ImageBitmap.toByteArray(quality: Int, maxSize: Int = Int.MAX_VALUE): ByteArray
