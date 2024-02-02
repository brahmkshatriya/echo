package dev.brahmkshatriya.echo.data.clients

import dev.brahmkshatriya.echo.data.models.StreamableAudio
import dev.brahmkshatriya.echo.data.models.Track

interface TrackClient {
    suspend fun getStreamable(track: Track): StreamableAudio

}