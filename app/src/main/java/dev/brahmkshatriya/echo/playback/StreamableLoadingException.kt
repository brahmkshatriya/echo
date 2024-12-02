package dev.brahmkshatriya.echo.playback

import androidx.media3.common.MediaItem
import dev.brahmkshatriya.echo.ui.exception.AppException
import kotlinx.io.IOException

class StreamableLoadingException(
    val mediaItem: MediaItem,
    override val cause: AppException
) : IOException()