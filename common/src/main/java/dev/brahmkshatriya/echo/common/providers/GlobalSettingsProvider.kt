package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.settings.Settings

/**
 * Interface to provide global [Settings] to the extension
 */
interface GlobalSettingsProvider {
    /**
     * Called when the extension is initialized, to provide the global [Settings] to the extension
     */
    fun setGlobalSettings(globalSettings: Settings)
}