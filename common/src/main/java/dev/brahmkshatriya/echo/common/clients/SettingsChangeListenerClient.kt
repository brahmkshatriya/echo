package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.providers.SettingsProvider
import dev.brahmkshatriya.echo.common.settings.Settings

interface SettingsChangeListenerClient : SettingsProvider {
    suspend fun onSettingsChanged(settings: Settings, key: String?)
}