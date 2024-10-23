package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.settings.Settings

fun getSettings(context: Context, type: ExtensionType, metadata: Metadata): Settings {
    val name = "$type-${metadata.id}"
    val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    return toSettings(prefs)
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