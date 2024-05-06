package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Lyrics

interface LyricsSearchClient : LyricsClient {
    fun searchLyrics(query: String): PagedData<Lyrics>
}