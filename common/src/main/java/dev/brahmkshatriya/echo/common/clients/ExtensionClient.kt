package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.models.ExtensionMetadata

interface ExtensionClient {
    fun getMetadata(): ExtensionMetadata
}