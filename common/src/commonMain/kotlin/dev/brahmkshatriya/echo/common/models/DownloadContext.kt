package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

/**
 * Download context for a track
 *
 * @param extensionId The extension that the track belongs to.
 * @param track The track to download.
 * @param sortOrder The order of the [track] in the [context].
 * @param context The context of the media item, Album/Playlist/Artist.
 */
@Serializable
data class DownloadContext(
    val extensionId: String,
    val track: Track,
    val sortOrder: Int? = null,
    val context: EchoMediaItem? = null
)
