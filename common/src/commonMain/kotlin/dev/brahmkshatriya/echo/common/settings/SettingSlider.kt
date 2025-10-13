package dev.brahmkshatriya.echo.common.settings

/**
 * A slider that allows the user to select a value from a range.
 * Use [allowOverride] to allow the user to use values outside the range.
 * Value can be accessed from [Settings.getInt]
 *
 * @param title The title of the setting.
 * @param key The key of the setting.
 * @param summary The summary of the setting.
 * @param defaultValue The default value of the setting.
 * @param from The minimum value of the slider.
 * @param to The maximum value of the slider.
 * @param steps The number of steps between the minimum and maximum values, if null, the slider will be continuous.
 * @param allowOverride Whether the user can use values outside the range.
 */
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