package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.models.Lyrics.Lyric
import kotlinx.serialization.Serializable

/**
 * Represents lyrics of a song, can be loaded later in [LyricsClient.loadLyrics].
 *
 * @property id the unique identifier of the lyrics.
 * @property title the title of the lyrics.
 * @property subtitle the subtitle of the lyrics.
 * @property lyrics the lyrics of the song.
 * @property extras additional information about the lyrics.
 *
 * @see Lyric
 */
@Serializable
data class Lyrics(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val lyrics: Lyric? = null,
    val extras: Map<String, String> = emptyMap()
) {

    /**
     * Represents a lyric of a song.
     *
     * This can be a [Simple] lyric or a [Timed] lyric.
     *
     * @see Simple
     * @see Timed
     */
    @Serializable
    sealed class Lyric

    /**
     * Represents a simple lyric of a song.
     *
     * @property text the text of the lyric.
     */
    @Serializable
    data class Simple(val text: String) : Lyric()

    /**
     * Represents a timed lyric of a song.
     *
     * @property list the list of timed lyric items.
     * @property fillTimeGaps whether to fill the time gaps between the items.
     *
     * @see Item
     */
    @Serializable
    data class Timed(
        val list: List<Item>,
        val fillTimeGaps: Boolean = true
    ) : Lyric()

    /**
     * Represents a timed lyric item.
     *
     * @property text the text of the lyric.
     * @property startTime the start time of the lyric.
     * @property endTime the end time of the lyric.
     */
    @Serializable
    data class Item(
        val text: String,
        val startTime: Long,
        val endTime: Long
    )
}
