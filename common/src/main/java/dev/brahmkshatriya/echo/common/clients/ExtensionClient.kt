package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings

interface ExtensionClient {
    val settingItems: List<Setting>
    fun setSettings(settings: Settings)
    suspend fun onExtensionSelected()
}