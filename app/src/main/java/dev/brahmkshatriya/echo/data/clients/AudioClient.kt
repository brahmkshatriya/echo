package dev.brahmkshatriya.echo.data.clients

import dev.brahmkshatriya.echo.data.models.StreamableAudio
import dev.brahmkshatriya.echo.data.models.Track

interface AudioClient {
    suspend fun getStreamable(track: Track.Full): StreamableAudio
}