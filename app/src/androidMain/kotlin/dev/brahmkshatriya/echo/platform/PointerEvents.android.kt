package dev.brahmkshatriya.echo.platform

import androidx.compose.ui.Modifier

actual fun Modifier.onPointerScrollY(onScroll: (Float) -> Unit): Modifier {
    return this
}