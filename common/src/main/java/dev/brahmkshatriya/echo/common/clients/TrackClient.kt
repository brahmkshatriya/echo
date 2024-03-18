package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Track

interface TrackClient {
    suspend fun getTrack(id: String): Track?
}