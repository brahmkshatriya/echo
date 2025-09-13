package dev.brahmkshatriya.echo.common.settings

import dev.brahmkshatriya.echo.common.providers.SettingsProvider

/**
 * Represents a setting that can be clicked to perform an action.
 * The [SettingsProvider.getSettingItems] will be called again after the action is performed.
 *
 * @property title The title of the setting.
 * @property key The unique key for the setting.
 * @property summary An optional summary for the setting.
 * @property onClick The action to perform when the setting is clicked.
 */
data class SettingOnClick(
    override val title: String,
    override val key: String,
    val summary: String? = null,
    val onClick: suspend () -> Unit,
) : Setting
