package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.EchoMediaItem

interface ShareClient {
    suspend fun onShare(item: EchoMediaItem): String
}