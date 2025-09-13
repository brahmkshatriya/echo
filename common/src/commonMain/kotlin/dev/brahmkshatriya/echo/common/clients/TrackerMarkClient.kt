package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.TrackDetails

/**
 * An interface for a tracker client that marks tracks as played after a certain duration.
 *
 * This interface extends [TrackerClient] and provides methods to determine when a track should be
 * marked as played based on its duration.
 */
interface TrackerMarkClient : TrackerClient {

    /**
     * The duration in milliseconds after which the track should be marked as played (defaults to null).
     * If null, the [onMarkAsPlayed] method will not be called.
     *
     * If you want a percentage of the song, you can calculate it based on the track's duration in
     * [onTrackChanged]
     */
    suspend fun getMarkAsPlayedDuration(details: TrackDetails): Long?

    /**
     * Called when the track has reached the [getMarkAsPlayedDuration].
     * will not be called if [getMarkAsPlayedDuration] is null.
     *
     * @param details the details of the track that is playing.
     */
    suspend fun onMarkAsPlayed(details: TrackDetails)
}