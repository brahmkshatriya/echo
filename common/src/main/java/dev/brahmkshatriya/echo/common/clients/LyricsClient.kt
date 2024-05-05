package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Lyric
import dev.brahmkshatriya.echo.common.models.LyricsItem
import dev.brahmkshatriya.echo.common.models.Track

interface LyricsClient {
    suspend fun searchTrackLyrics(clientId: String, track: Track): PagedData<LyricsItem>
    suspend fun getLyrics(item: LyricsItem): List<Lyric>
}