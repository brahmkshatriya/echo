package dev.brahmkshatriya.echo.app.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.onPointerScrollY(onScroll: (Float) -> Unit): Modifier {
    return onPointerEvent(PointerEventType.Scroll) {
        onScroll(it.changes.first().scrollDelta.y)
    }
}