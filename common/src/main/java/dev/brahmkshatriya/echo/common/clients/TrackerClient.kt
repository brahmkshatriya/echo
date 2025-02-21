package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.TrackerExtension
import dev.brahmkshatriya.echo.common.models.TrackDetails

/**
 * Used to track the playback of a track.
 *
 * You can override the following methods to get the track details:
 * - [onTrackChanged]
 * - [onPlayingStateChanged]
 * - [onMarkAsPlayed]
 *
 * Can be implemented by both:
 * - [MusicExtension]
 * - [TrackerExtension]
 */
interface TrackerClient : ExtensionClient {

    /**
     * Called when the track has started playing.
     *
     * This method will be called again if the track was played again from the beginning.
     *
     * @param details the details of the track that is playing, or null if player is empty.
     */
    suspend fun onTrackChanged(details: TrackDetails?) {}

    /**
     * The duration in milliseconds after which the track should be marked as played (defaults to null).
     * If null, the [onMarkAsPlayed] method will not be called.
     */
    val markAsPlayedDuration: Long?
        get() = null

    /**
     * Called when the track has reached the [markAsPlayedDuration].
     * will not be called if [markAsPlayedDuration] is null.
     *
     * @param details the details of the track that is playing.
     */
    suspend fun onMarkAsPlayed(details: TrackDetails) {}


    /**
     * Called when the track has started playing or paused.
     *
     * @param details the details of the track that was playing.
     */
    suspend fun onPlayingStateChanged(details: TrackDetails, isPlaying: Boolean) {}
}