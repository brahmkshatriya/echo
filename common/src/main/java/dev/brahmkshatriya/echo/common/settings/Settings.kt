package dev.brahmkshatriya.echo.common.settings

/**
 * Interface to provide settings to the extension.
 * This can be used to store data that needs to be persisted. (Recommended to not store sensitive data)
 *
 * Following [Setting] Can be used for following types of data:
 * - String: [SettingTextInput], [SettingList]
 * - String Set: [SettingMultipleChoice]
 * - Integer: [SettingSlider]
 * - Boolean: [SettingSwitch]
 */
interface Settings {
    /**
     * Get the [String] value for the given key. Returns null if the key is not found.
     */
    fun getString(key: String): String?

    /**
     * Store the value for the given key.
     * @param value: The value to store. If null, the key will be removed.
     */
    fun putString(key: String, value: String?)

    /**
     * Get the [Set] of [String]s for the given key. Returns null if the key is not found.
     */
    fun getStringSet(key: String): Set<String>?

    /**
     * Store the value for the given key.
     * @param value: The value to store. If null, the key will be removed.
     */
    fun putStringSet(key: String, value: Set<String>?)

    /**
     * Get the [Int] value for the given key. Returns null if the key is not found.
     */
    fun getInt(key: String): Int?

    /**
     * Store the value for the given key.
     * @param value: The value to store. If null, the key will be removed.
     */
    fun putInt(key: String, value: Int?)

    /**
     * Get the [Boolean] value for the given key. Returns null if the key is not found.
     */
    fun getBoolean(key: String): Boolean?

    /**
     * Store the value for the given key.
     * @param value: The value to store. If null, the key will be removed.
     */
    fun putBoolean(key: String, value: Boolean?)
}