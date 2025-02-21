package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Lyrics

/**
 * Used to search for lyrics.
 *
 * @see Lyrics
 * @see PagedData
 */
interface LyricsSearchClient : LyricsClient {
    /**
     * Searches for lyrics.
     *
     * @param query the query to search for.
     * @return the paged lyrics.
     *
     * @see Lyrics
     * @see PagedData
     */
    fun searchLyrics(query: String): PagedData<Lyrics>
}