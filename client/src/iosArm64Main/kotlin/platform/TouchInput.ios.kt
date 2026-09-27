package dev.brahmkshatriya.echo.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@get:Composable
actual val hasTouchInput: State<Boolean>
    get() = remember { mutableStateOf(true) }
