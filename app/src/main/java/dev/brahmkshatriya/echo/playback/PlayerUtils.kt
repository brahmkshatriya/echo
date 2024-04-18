package dev.brahmkshatriya.echo.playback

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import dev.brahmkshatriya.echo.common.models.Track
import kotlin.math.roundToLong

fun mediaItemBuilder(
    track: Track
): MediaItem {
    val item = MediaItem.Builder()
    item.setUri(track.id)
    val metadata = track.toMetaData()
    item.setMediaMetadata(metadata)

    val mediaId = track.id
    item.setMediaId(mediaId)
    return item.build()
}

private fun Track.toMetaData() = MediaMetadata.Builder()
    .setTitle(title)
    .setArtist(artists.firstOrNull()?.name)
    .setArtworkUri(id.toUri())
    .setIsPlayable(true)
    .setIsBrowsable(false)
    .build()


fun Long.toTimeString(): String {
    val seconds = (this.toFloat() / 1000).roundToLong()
    val minutes = seconds / 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
    } else {
        String.format("%02d:%02d", minutes, seconds % 60)
    }
}

