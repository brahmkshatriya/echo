package dev.brahmkshatriya.echo.common.settings

data class SettingTextInput(
    override val title: String,
    override val key: String,
    val summary: String? = null,
    val defaultValue: String? = null
) : Setting