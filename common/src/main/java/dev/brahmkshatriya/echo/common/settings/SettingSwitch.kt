package dev.brahmkshatriya.echo.common.settings

/**
 * A switch that allows the user to toggle a setting on or off.
 * Value can be accessed from [Settings.getBoolean]
 *
 * @param title The title of the setting.
 * @param key The key of the setting.
 * @param summary The summary of the setting.
 * @param defaultValue The default value of the setting.
 */
data class SettingSwitch(
    override val title: String,
    override val key: String,
    val summary: String? = null,
    val defaultValue: Boolean
) : Setting