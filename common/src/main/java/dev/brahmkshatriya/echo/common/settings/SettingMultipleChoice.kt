package dev.brahmkshatriya.echo.common.settings

data class SettingMultipleChoice(
    override val title: String,
    override val key: String,
    val summary: String? = null,
    val entryTitles: List<String>,
    val entryValues: List<String>,
    val defaultEntryIndices: Set<Int>? = null
) : Setting