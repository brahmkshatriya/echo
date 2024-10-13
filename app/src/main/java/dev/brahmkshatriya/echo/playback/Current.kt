package dev.brahmkshatriya.echo.playback

import androidx.media3.common.MediaItem
import dev.brahmkshatriya.echo.playback.MediaItemUtils.context
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track

data class Current(
    val index: Int,
    val mediaItem: MediaItem,
    val isLoaded: Boolean,
    val isPlaying: Boolean,
) {
    val context = lazy { mediaItem.context }
    val track = lazy { mediaItem.track }
    fun isPlaying(id: String): Boolean {
        val same = mediaItem.mediaId == id
                || context.value?.id == id
                || track.value.album?.id == id
                || track.value.artists.any { it.id == id }
        return isPlaying && same
    }

    companion object {
        fun Current?.isPlaying(id: String): Boolean = this?.isPlaying(id) ?: false
    }
}
