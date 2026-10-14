package dev.brahmkshatriya.echo.app.platform

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import platform.UIKit.UIDevice
import platform.UIKit.UIApplication

private class IosPlatform : Platform {
    override val name: String = buildString {
        append(UIDevice.currentDevice.systemName)
        append(' ')
        append(UIDevice.currentDevice.systemVersion)
    }
}

actual fun getPlatform(): Platform = IosPlatform()

@get:Composable
actual val hasTouchInput: State<Boolean>
    get() = remember { mutableStateOf(true) }

@Composable
internal actual fun imeAnimationInsetsOrNull(): Pair<WindowInsets, WindowInsets>? = null

@Composable
internal actual fun KeepScreenAwake(enabled: Boolean) {
    DisposableEffect(enabled) {
        val application = UIApplication.sharedApplication
        val wasDisabled = application.idleTimerDisabled
        if (enabled) application.idleTimerDisabled = true
        onDispose {
            if (enabled) application.idleTimerDisabled = wasDisabled
        }
    }
}
