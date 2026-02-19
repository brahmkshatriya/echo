@file:OptIn(ExperimentalComposeUiApi::class)

package dev.brahmkshatriya.echo.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent

actual fun Modifier.onPointerScrollY(onScroll: (Float) -> Unit): Modifier {
    return onPointerEvent(PointerEventType.Scroll) {
        onScroll(it.changes.first().scrollDelta.y)
    }
}