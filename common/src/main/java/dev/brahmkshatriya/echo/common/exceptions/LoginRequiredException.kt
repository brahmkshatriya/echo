package dev.brahmkshatriya.echo.common.exceptions

import dev.brahmkshatriya.echo.common.clients.ExtensionClient

data class LoginRequiredException(
    val clientId: String,
    val clientName: String
) : Exception("Login Required ($clientId : $clientName)") {
    companion object {
        fun from(extensionClient: ExtensionClient) =
            LoginRequiredException(extensionClient.metadata.id, extensionClient.metadata.name)
    }
}