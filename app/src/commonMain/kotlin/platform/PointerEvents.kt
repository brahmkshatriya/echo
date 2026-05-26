package dev.brahmkshatriya.echo.app.platform

import androidx.compose.ui.Modifier

expect fun Modifier.onPointerScrollY(onScroll: (Float) -> Unit) : Modifier