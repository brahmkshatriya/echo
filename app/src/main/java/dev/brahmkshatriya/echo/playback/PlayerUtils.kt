package dev.brahmkshatriya.echo.playback

import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
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

@OptIn(UnstableApi::class)
fun DataSpec.copy(
    uri: Uri? = null,
    uriPositionOffset : Long? = null,
    httpMethod : Int? = null,
    httpBody : ByteArray? = null,
    httpRequestHeaders : Map<String, String>? = null,
    position : Long? = null,
    length : Long? = null,
    key : String? = null,
    flags : Int? = null,
    customData : Any? = null
): DataSpec {
    return DataSpec.Builder()
        .setUri(uri ?: this.uri)
        .setUriPositionOffset(uriPositionOffset ?: this.uriPositionOffset)
        .setHttpMethod(httpMethod ?: this.httpMethod)
        .setHttpBody(httpBody ?: this.httpBody)
        .setHttpRequestHeaders(httpRequestHeaders ?: this.httpRequestHeaders)
        .setPosition(position ?: this.position)
        .setLength(length ?: this.length)
        .setKey(key ?: this.key)
        .setFlags(flags ?: this.flags)
        .setCustomData(customData ?: this.customData)
        .build()
}
