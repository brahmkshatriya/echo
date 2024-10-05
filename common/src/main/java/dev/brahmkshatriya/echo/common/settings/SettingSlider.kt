package dev.brahmkshatriya.echo.common.settings

data class SettingSlider (
    override val title: String,
    override val key: String,
    val summary: String? = null,
    val defaultValue: Int? = null,
    val from: Int,
    val to: Int,
    val steps: Int? = null,
    val allowOverride: Boolean = false
) : Setting