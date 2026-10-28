package dev.brahmkshatriya.echo.app.platform

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import sdl3.SDL_DisableScreenSaver
import sdl3.SDL_EnableScreenSaver
import sdl3.SDL_GetTouchDeviceType
import sdl3.SDL_GetTouchDevices
import sdl3.SDL_ScreenSaverEnabled
import sdl3.SDL_SetClipboardText
import sdl3.SDL_TOUCH_DEVICE_DIRECT
import sdl3.SDL_free
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform as NativePlatform

class DesktopNativePlatform : Platform {
    @OptIn(ExperimentalNativeApi::class)
    override val name: String = "Kotlin/Native ${NativePlatform.osFamily.name}"
}

actual fun getPlatform(): Platform = DesktopNativePlatform()

@get:Composable
actual val hasTouchInput: State<Boolean>
    get() {
        val isWindowFocused = LocalWindowInfo.current.isWindowFocused
        val state = remember { mutableStateOf(queryHasTouchInput()) }
        LaunchedEffect(isWindowFocused) {
            if (isWindowFocused) state.value = queryHasTouchInput()
        }
        return state
    }

@OptIn(ExperimentalForeignApi::class)
private fun queryHasTouchInput(): Boolean = memScoped {
    val count = alloc<IntVar>()
    val devices = SDL_GetTouchDevices(count.ptr) ?: return@memScoped false
    try {
        (0 until count.value).any { index ->
            SDL_GetTouchDeviceType(devices[index]) == SDL_TOUCH_DEVICE_DIRECT
        }
    } finally {
        SDL_free(devices)
    }
}

@Composable
internal actual fun imeAnimationInsetsOrNull(): Pair<WindowInsets, WindowInsets>? = null

@Composable
@OptIn(ExperimentalForeignApi::class)
internal actual fun KeepScreenAwake(enabled: Boolean) {
    DisposableEffect(enabled) {
        val wasEnabled = SDL_ScreenSaverEnabled()
        if (enabled) SDL_DisableScreenSaver()
        onDispose { if (enabled && wasEnabled) SDL_EnableScreenSaver() }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun Clipboard.setPlainText(text: String) {
    SDL_SetClipboardText(text)
}
