package dev.brahmkshatriya.echo.plugger

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.utils.mapState
import tel.jeelpa.plugger.PluginRepo

inline fun <reified T : ExtensionClient> Context.injectSettings(
    type: ExtensionType,
    pluginRepo: PluginRepo<ExtensionMetadata, T>
) = pluginRepo.getAllPlugins().mapState { list ->
    list.map {
        runCatching {
            it.getOrThrow().apply {
                val settings = toSettings(
                    getSharedPreferences("$type-${first.id}", Context.MODE_PRIVATE)
                )
                second.setSettings(settings)
            }
        }
    }
}

fun toSettings(prefs: SharedPreferences) = object : Settings {
    override fun getString(key: String) = prefs.getString(key, null)
    override fun putString(key: String, value: String?) {
        prefs.edit { putString(key, value) }
    }

    override fun getInt(key: String) = if (prefs.contains(key)) prefs.getInt(key, 0)
    else null

    override fun putInt(key: String, value: Int?) {
        prefs.edit { putInt(key, value) }
    }

    override fun getBoolean(key: String) = if (prefs.contains(key)) prefs.getBoolean(key, false)
    else null

    override fun putBoolean(key: String, value: Boolean?) {
        prefs.edit { putBoolean(key, value) }
    }
}