package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.providers.SettingsProvider

interface ExtensionClient : SettingsProvider {
    suspend fun onExtensionSelected()
}