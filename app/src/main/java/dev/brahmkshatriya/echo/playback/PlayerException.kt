package dev.brahmkshatriya.echo.playback

import androidx.media3.common.MediaItem
import dev.brahmkshatriya.echo.ExceptionActivity

data class PlayerException(
    val details: ExceptionActivity.ExceptionDetails,
    val mediaItem: MediaItem?
) : Throwable()