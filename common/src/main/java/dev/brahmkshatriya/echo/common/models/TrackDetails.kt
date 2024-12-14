package dev.brahmkshatriya.echo.common.models

/**
 * A data class that holds the details of a track that is in the player.
 *
 * @param extensionId the extension id of the extension that the track is from.
 * @param context the context of the track.
 * @param track the track itself.
 *
 * @see EchoMediaItem
 * @see Track
 */
data class TrackDetails(
    val extensionId: String,
    val track: Track,
    val context: EchoMediaItem?
)