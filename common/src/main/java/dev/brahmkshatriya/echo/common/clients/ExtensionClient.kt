package dev.brahmkshatriya.echo.common.clients

import android.content.SharedPreferences
import androidx.preference.PreferenceScreen
import dev.brahmkshatriya.echo.common.models.ExtensionMetadata

abstract class ExtensionClient {
    abstract val metadata: ExtensionMetadata
    abstract fun setupPreferenceSettings(preferenceScreen: PreferenceScreen)

    val preferences get() = _preferences!!

    private var _preferences: SharedPreferences? = null
    fun setPreferences(preferences: SharedPreferences) {
        _preferences = preferences
    }
}