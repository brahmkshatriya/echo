package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.StreamableVideo
import dev.brahmkshatriya.echo.common.models.Track

interface TrackClient {
    suspend fun loadTrack(track: Track): Track
    suspend fun getStreamableAudio(streamable: Streamable): StreamableAudio
    suspend fun getStreamableVideo(streamable: Streamable): StreamableVideo
    fun getMediaItems(track: Track): PagedData<MediaItemsContainer>
}