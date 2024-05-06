package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track

interface TrackerClient : ExtensionClient {
    suspend fun onStartedPlaying(clientId: String, context: EchoMediaItem?, track: Track)
    suspend fun onMarkAsPlayed(clientId: String, context: EchoMediaItem?, track: Track)
    suspend fun onStoppedPlaying(clientId: String, context: EchoMediaItem?, track: Track)
}