package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Track

interface LyricsClient : ExtensionClient {
    fun searchTrackLyrics(clientId: String, track: Track): PagedData<Lyrics>
    suspend fun loadLyrics(small: Lyrics): Lyrics
}