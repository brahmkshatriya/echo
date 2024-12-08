package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.TrackerExtension
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track

/**
 * Used to track the playback of a track.
 *
 * Can be implemented by both:
 * - [MusicExtension]
 * - [TrackerExtension]
 */
interface TrackerClient : ExtensionClient {

    /**
     * Called when the track has started playing.
     *
     * @param extensionId the extension id of the extension that is playing the track.
     * @param context the context of the track.
     * @param track the track that is playing.
     *
     * @see EchoMediaItem
     * @see Track
     */
    suspend fun onStartedPlaying(extensionId: String, context: EchoMediaItem?, track: Track)

    /**
     * Called when the track has reached the mark as played duration (defaults to 30s).
     *
     * @param extensionId the extension id of the extension that is playing the track.
     * @param context the context of the track.
     * @param track the track that is playing.
     *
     * @see EchoMediaItem
     * @see Track
     */
    suspend fun onMarkAsPlayed(extensionId: String, context: EchoMediaItem?, track: Track)

    /**
     * Called when the track has stopped playing.
     *
     * @param extensionId the extension id of the extension that is playing the track.
     * @param context the context of the track.
     * @param track the track that is playing.
     *
     * @see EchoMediaItem
     * @see Track
     */
    suspend fun onStoppedPlaying(extensionId: String, context: EchoMediaItem?, track: Track)
}