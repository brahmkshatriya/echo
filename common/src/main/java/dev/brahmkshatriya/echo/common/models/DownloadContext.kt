package dev.brahmkshatriya.echo.common.models

/**
 * Download context for a track
 *
 * @param extensionId The extension that the track belongs to.
 * @param track The track to download.
 * @param context The context of the media item, Album/Playlist/Artist.
 */
data class DownloadContext(
    val extensionId: String,
    val track: Track,
    val context: EchoMediaItem?
)
