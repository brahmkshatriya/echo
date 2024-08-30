package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track

interface TrackClient {
    suspend fun loadTrack(track: Track): Track
    suspend fun getStreamableMedia(streamable: Streamable): Streamable.Media
    fun getMediaItems(track: Track): PagedData<MediaItemsContainer>
}