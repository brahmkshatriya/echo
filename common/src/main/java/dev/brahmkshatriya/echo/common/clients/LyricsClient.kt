package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.LyricsExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Track

/**
 * Used to get the lyrics for track.
 *
 * Can be implemented by both:
 * - [MusicExtension]
 * - [LyricsExtension]
 *
 * To support lyrics search from user query. Use [LyricsSearchClient] instead.
 *
 * @see Lyrics
 * @see Track
 * @see PagedData
 */
interface LyricsClient : ExtensionClient {

    /**
     * Searches for the unloaded lyrics of a track.
     *
     * @param clientId the client id to use for the search.
     * @param track the track to search the lyrics for.
     * @return the paged lyrics.
     *
     * @see Lyrics
     * @see Track
     * @see PagedData
     */
    fun searchTrackLyrics(clientId: String, track: Track): PagedData<Lyrics>

    /**
     * Loads the unloaded lyrics.
     *
     * @param lyrics the lyrics to load.
     * @return the loaded lyrics.
     *
     * @see Lyrics
     */
    suspend fun loadLyrics(lyrics: Lyrics): Lyrics
}