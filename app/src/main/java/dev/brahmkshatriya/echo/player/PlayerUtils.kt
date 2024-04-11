package dev.brahmkshatriya.echo.player

import android.graphics.Bitmap
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.Track
import java.nio.ByteBuffer
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
    .setArtwork(cover)
    .setIsPlayable(true)
    .setIsBrowsable(false)
    .build()

private fun MediaMetadata.Builder.setArtwork(cover: ImageHolder?): MediaMetadata.Builder {
    cover?.let {
        return when (it) {
            is ImageHolder.UrlImageHolder -> setArtworkUri(it.urlHolder.url.toUri())
            is ImageHolder.UriImageHolder -> setArtworkUri(it.uri)
            is ImageHolder.BitmapImageHolder -> setArtworkData(
                it.bitmap.toByteArray(),
                MediaMetadata.PICTURE_TYPE_FILE_ICON
            )
        }
    }
    return this
}

private fun Bitmap.toByteArray(): ByteArray {
    val size: Int = rowBytes * height
    val byteBuffer = ByteBuffer.allocate(size)
    copyPixelsToBuffer(byteBuffer)
    return byteBuffer.array()
}


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

