package dev.brahmkshatriya.echo.plugger

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.providers.ContextProvider
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.utils.mapState
import kotlinx.coroutines.flow.StateFlow

inline fun <reified T : ExtensionClient> StateFlow<List<Result<Pair<ExtensionMetadata, T>>>>.injectSettings(
    type: ExtensionType,
    context: Context
) = mapState { list ->
    list.map {
        runCatching {
            it.getOrThrow().apply {
                val settings = toSettings(
                    context.getSharedPreferences("$type-${first.id}", Context.MODE_PRIVATE)
                )
                second.setSettings(settings)
            }
        }
    }
}

inline fun <reified T : ExtensionClient> StateFlow<List<Result<Pair<ExtensionMetadata, T>>>>.injectContext(
    context: Context
) = mapState { list ->
    list.map {
        runCatching {
            it.getOrThrow().apply {
                (second as? ContextProvider)?.setContext(context)
            }
        }
    }
}

fun toSettings(prefs: SharedPreferences) = object : Settings {
    override fun getString(key: String) = prefs.getString(key, null)
    override fun putString(key: String, value: String?) {
        prefs.edit { putString(key, value) }
    }

    override fun getInt(key: String) =
        if (prefs.contains(key)) prefs.getInt(key, 0) else null

    override fun putInt(key: String, value: Int?) {
        prefs.edit { putInt(key, value) }
    }

    override fun getBoolean(key: String) =
        if (prefs.contains(key)) prefs.getBoolean(key, false) else null

    override fun putBoolean(key: String, value: Boolean?) {
        prefs.edit { putBoolean(key, value) }
    }

    override fun getStringSet(key: String) = prefs.getStringSet(key, null)
    override fun putStringSet(key: String, value: Set<String>?) {
        prefs.edit { putStringSet(key, value) }
    }
}