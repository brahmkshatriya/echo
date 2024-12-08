package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings

/**
 * Interface to provide [Settings] to the extension
 */
interface SettingsProvider {
    /**
     * List of [Setting]s to be displayed in the settings screen
     */
    val settingItems: List<Setting>
    /**
     * Called when the extension is initialized, to provide the [Settings] to the extension
     */
    fun setSettings(settings: Settings)
}