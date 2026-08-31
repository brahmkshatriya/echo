package dev.brahmkshatriya.echo.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

@get:Composable
expect val hasTouchInput: State<Boolean>
