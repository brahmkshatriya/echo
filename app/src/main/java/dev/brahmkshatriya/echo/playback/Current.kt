package dev.brahmkshatriya.echo.playback

import androidx.media3.common.MediaItem

data class Current(
    val index: Int,
    val mediaItem: MediaItem,
    val isLoaded: Boolean
)
