package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.models.GlobalSettings

/**
 * Interface to provide [GlobalSettings] to the extension
 */
interface GlobalSettingsProvider {
    /**
     * Called when the extension is initialized, to provide the [GlobalSettings] to the extension
     */
    fun setGlobalSettings(globalSettings: GlobalSettings)
}