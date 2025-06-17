package dev.brahmkshatriya.echo.common.models

/**
 * A data class that holds the details of a track that is in the player.
 *
 * @param extensionId the extension id of the extension that the track is from.
 * @param context the context of the track.
 * @param track the track itself.
 * @param currentPosition the current position of the track in milliseconds.
 * @param totalDuration the total duration of the track in milliseconds, or null if unknown.
 *
 * @see EchoMediaItem
 * @see Track
 */
data class TrackDetails(
    val extensionId: String,
    val track: Track,
    val context: EchoMediaItem?,
    val currentPosition: Long,
    val totalDuration: Long?,
)