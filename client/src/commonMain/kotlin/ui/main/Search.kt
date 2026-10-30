package dev.brahmkshatriya.echo.app.ui.main

import androidx.compose.runtime.Composable
import dev.brahmkshatriya.echo.app.ui.LocalPlaybackComponent
import dev.brahmkshatriya.echo.app.ui.Test


@Composable
fun Search() {
    Test("Search", LocalPlaybackComponent.current.session::ensureQueue)
}