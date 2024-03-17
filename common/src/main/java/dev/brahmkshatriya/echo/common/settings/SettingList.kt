package dev.brahmkshatriya.echo.common.settings

data class SettingList(
    override val title: String,
    override val key: String,
    val summary: String? = null,
    val entryTitles: List<String>,
    val entryValues: List<String>,
    val defaultEntryIndex: Int? = null
) : Setting