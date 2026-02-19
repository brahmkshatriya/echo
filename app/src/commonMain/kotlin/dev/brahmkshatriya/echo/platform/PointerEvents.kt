package dev.brahmkshatriya.echo.platform

import androidx.compose.ui.Modifier

expect fun Modifier.onPointerScrollY(onScroll: (Float) -> Unit) : Modifier