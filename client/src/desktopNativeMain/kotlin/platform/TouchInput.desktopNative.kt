package dev.brahmkshatriya.echo.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import sdl3.SDL_GetTouchDeviceType
import sdl3.SDL_GetTouchDevices
import sdl3.SDL_TOUCH_DEVICE_DIRECT
import sdl3.SDL_free

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
