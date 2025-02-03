package dev.brahmkshatriya.echo.common.settings

data class SettingOnClick(
    override val title: String,
    override val key: String,
    val summary: String? = null,
    val onClick: () -> Unit,
) : Setting
