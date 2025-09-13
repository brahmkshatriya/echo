package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track

/**
 * Used to load track and their media to allow streaming.
 *
 * @see MusicExtension
 */
interface TrackClient {

    /**
     * Loads an unloaded track.
     * Make sure the [track] contains at least one [Track.servers] in [Track.streamables]
     *
     * @param track the track to load.
     * @param isDownload whether the track is being downloaded.
     * @return the loaded track.
     *
     * @see Track
     */
    suspend fun loadTrack(track: Track, isDownload: Boolean): Track

    /**
     * Loads the media of a streamable.
     *
     * @param streamable the streamable to load the media of.
     * @param isDownload whether the media is being downloaded.
     * @return the media of the streamable.
     *
     * @see Streamable
     * @see Streamable.Media
     */
    suspend fun loadStreamableMedia(streamable: Streamable, isDownload: Boolean): Streamable.Media

    /**
     * Gets the shelves of a track.
     *
     * @param track the track to get the shelves of.
     * @return the feed containing the shelves, or null if not available.
     *
     * @see Feed
     */
    suspend fun loadFeed(track: Track): Feed<Shelf>?
}