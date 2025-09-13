package dev.brahmkshatriya.echo.common.settings

/**
 * A setting that allows the user to input a string.
 * Value can be accessed from [Settings.getString], recommended to use [String.isNullOrBlank].
 *
 * @param title The title of the setting.
 * @param key The key of the setting.
 * @param summary The summary of the setting.
 * @param defaultValue The default value of the setting.
 */
data class SettingTextInput(
    override val title: String,
    override val key: String,
    val summary: String? = null,
    val defaultValue: String? = null
) : Setting