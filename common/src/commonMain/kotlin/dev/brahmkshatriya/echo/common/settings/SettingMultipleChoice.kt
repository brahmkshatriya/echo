package dev.brahmkshatriya.echo.common.settings

/**
 * A setting that allows the user to select multiple items from a list of options.
 * Values can be accessed from [Settings.getStringSet]
 *
 * @param title The title of the setting.
 * @param key The unique key of the setting.
 * @param summary The summary of the setting.
 * @param entryTitles The titles of the entries.
 * @param entryValues The values of the entries.
 * @param defaultEntryIndices The indices of the default entries.
 */
data class SettingMultipleChoice(
    override val title: String,
    override val key: String,
    val summary: String? = null,
    val entryTitles: List<String>,
    val entryValues: List<String>,
    val defaultEntryIndices: Set<Int>? = null
) : Setting