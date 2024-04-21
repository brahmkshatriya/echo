package dev.brahmkshatriya.echo.common.clients

import android.content.SharedPreferences
import dev.brahmkshatriya.echo.common.models.ExtensionMetadata
import dev.brahmkshatriya.echo.common.settings.Setting

abstract class ExtensionClient {
    abstract val metadata: ExtensionMetadata
    abstract val settings: List<Setting>

    val preferences get() = _preferences!!

    private var _preferences: SharedPreferences? = null
    fun setPreferences(preferences: SharedPreferences) {
        _preferences = preferences
    }
    open suspend fun onExtensionSelected() {}
}