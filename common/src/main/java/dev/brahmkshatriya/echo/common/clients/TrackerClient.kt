package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Track

interface TrackerClient {
    suspend fun onStartedPlaying(clientId: String, track: Track)
    suspend fun onMarkAsPlayed(clientId: String, track: Track)
    suspend fun onStoppedPlaying(clientId: String, track: Track)
}