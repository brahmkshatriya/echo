package dev.brahmkshatriya.echo.common.settings

import dev.brahmkshatriya.echo.common.clients.SettingsChangeListenerClient

/**
 * A [Setting] item that can be used to show some data in the UI
 * or as a Button with the help of [SettingsChangeListenerClient].
 *
 * @param title The title of the setting.
 * @param key The unique key of the setting, will be called when the setting is clicked.
 * @param summary The summary of the setting.
 */
data class SettingItem(
    override val title: String,
    override val key: String,
    val summary: String? = null,
) : Setting