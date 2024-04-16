package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.Track

interface TrackerClient {
    fun onStartedPlaying(clientId: String, track: Track)
    fun onMarkAsPlayed(clientId: String, track: Track)
}