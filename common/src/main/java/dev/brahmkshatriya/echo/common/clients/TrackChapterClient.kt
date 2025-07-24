package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Chapter
import dev.brahmkshatriya.echo.common.models.Track

/**
 * Used to load [Chapter]s for a track.
 *
 * @see Track
 */
interface TrackChapterClient {

    /**
     * Gets the chapters for a track.
     *
     * @param track the track to get the chapters of.
     * @return the list of chapters for the track.
     *
     * @see Chapter
     */
    suspend fun getChapters(track: Track) : List<Chapter>
}