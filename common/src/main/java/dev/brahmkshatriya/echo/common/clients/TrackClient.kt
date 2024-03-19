package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.Track

interface TrackClient {
    suspend fun getTrack(id: String): Track?
    suspend fun getStreamableAudio(streamable: Streamable) : StreamableAudio
}