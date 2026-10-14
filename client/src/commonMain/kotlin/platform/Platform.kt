package dev.brahmkshatriya.echo.app.platform

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
@get:Composable
expect val hasTouchInput: State<Boolean>

/** Android exposes IME animation endpoints; other targets only expose IME insets. */
@Composable
internal expect fun imeAnimationInsetsOrNull(): Pair<WindowInsets, WindowInsets>?
/** Prevent screen timeout while the expanded player displays immersive content. */
@Composable
internal expect fun KeepScreenAwake(enabled: Boolean)