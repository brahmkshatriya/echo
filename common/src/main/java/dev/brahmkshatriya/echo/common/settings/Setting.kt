package dev.brahmkshatriya.echo.common.settings

/**
 * A sealed interface that represents a setting. These are the types of [Setting]s:
 * - [SettingCategory]
 * - [SettingItem]
 * - [SettingSwitch]
 * - [SettingSlider]
 * - [SettingTextInput]
 * - [SettingList]
 * - [SettingMultipleChoice]
 * - [SettingOnClick]
 *
 * @property title The title of the setting.
 * @property key The unique key of the setting.
 */
sealed interface Setting {
    val title: String
    val key: String
}