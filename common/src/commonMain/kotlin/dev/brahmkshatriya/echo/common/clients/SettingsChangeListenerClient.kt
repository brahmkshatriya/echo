package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.providers.SettingsProvider
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings

/**
 * A client that listens to changes in the extension's [Settings].
 */
interface SettingsChangeListenerClient : SettingsProvider {
    /**
     * Called when the extension's [Settings] have changed or when a [Setting] has been clicked.
     *
     * @param settings The new [Settings].
     * @param key The key of the setting that has changed or clicked. Null if all settings have changed.
     */
    suspend fun onSettingsChanged(settings: Settings, key: String?)
}