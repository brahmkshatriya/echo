package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings

interface SettingsProvider {
    val settingItems: List<Setting>
    fun setSettings(settings: Settings)
}