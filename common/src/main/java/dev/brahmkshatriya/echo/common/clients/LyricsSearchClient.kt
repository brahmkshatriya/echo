package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.LyricsItem

interface LyricsSearchClient {
    suspend fun searchLyrics(query: String): PagedData<LyricsItem>
}