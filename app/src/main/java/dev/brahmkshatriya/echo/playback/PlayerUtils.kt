package dev.brahmkshatriya.echo.playback

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.ThumbRating
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Track

val likeCommand = SessionCommand("liked", Bundle.EMPTY)
val unlikeCommand = SessionCommand("unliked", Bundle.EMPTY)
val repeatCommand = SessionCommand("repeat", Bundle.EMPTY)
val repeatOffCommand = SessionCommand("repeat_off", Bundle.EMPTY)
val repeatOneCommand = SessionCommand("repeat_one", Bundle.EMPTY)

fun getLikeButton(context: Context, liked: Boolean) = run {
    val builder = CommandButton.Builder()
    if (!liked) builder
        .setDisplayName(context.getString(R.string.like))
        .setIconResId(R.drawable.ic_heart_outline)
        .setSessionCommand(likeCommand)
    else builder
        .setDisplayName(context.getString(R.string.unlike))
        .setIconResId(R.drawable.ic_heart_filled)
        .setSessionCommand(unlikeCommand)
    builder.build()
}

fun getRepeatButton(context: Context, repeat: Int) = run {
    val builder = CommandButton.Builder()
    builder.setDisplayName(context.getString(R.string.repeat))
    when (repeat) {
        Player.REPEAT_MODE_ONE -> builder
            .setIconResId(R.drawable.ic_repeat_one)
            .setSessionCommand(repeatOffCommand)

        Player.REPEAT_MODE_OFF -> builder
            .setIconResId(R.drawable.ic_repeat_off)
            .setSessionCommand(repeatCommand)

        else -> builder
            .setIconResId(R.drawable.ic_repeat)
            .setSessionCommand(repeatOneCommand)
    }
    builder.build()
}

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

fun Track.toMetaData() = MediaMetadata.Builder()
    .setTitle(title)
    .setArtist(artists.firstOrNull()?.name)
    .setArtworkUri(id.toUri())
    .setUserRating(ThumbRating(liked))
    .setIsPlayable(true)
    .setIsBrowsable(false)
    .build()

@OptIn(UnstableApi::class)
fun DataSpec.copy(
    uri: Uri? = null,
    uriPositionOffset: Long? = null,
    httpMethod: Int? = null,
    httpBody: ByteArray? = null,
    httpRequestHeaders: Map<String, String>? = null,
    position: Long? = null,
    length: Long? = null,
    key: String? = null,
    flags: Int? = null,
    customData: Any? = null
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
