package dev.brahmkshatriya.echo.common.settings

/**
 * A setting that allows the user to select a single string from a list of options.
 * Value can be accessed from [Settings.getString]
 *
 * @param title The title of the setting.
 * @param key The key of the setting.
 * @param summary The summary of the setting.
 * @param entryTitles The titles of the entries.
 * @param entryValues The values of the entries.
 * @param defaultEntryIndex The index of the default entry.
 */
data class SettingList(
    override val title: String,
    override val key: String,
    val summary: String? = null,
    val entryTitles: List<String>,
    val entryValues: List<String>,
    val defaultEntryIndex: Int? = null
) : Setting