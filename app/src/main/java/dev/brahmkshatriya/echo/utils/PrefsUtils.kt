package dev.brahmkshatriya.echo.utils


import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.widget.Toast
import androidx.core.content.edit
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.extensionPrefId
import dev.brahmkshatriya.echo.playback.listener.EffectsListener.Companion.CUSTOM_EFFECTS
import dev.brahmkshatriya.echo.playback.listener.EffectsListener.Companion.GLOBAL_FX
import dev.brahmkshatriya.echo.utils.ContextUtils.SETTINGS_NAME
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.contentOrNull
import java.io.File
import java.io.FileWriter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date


private fun Map<String, Any?>.toJsonElementMap(): Map<String, JsonElement> {
    return this.mapValues { (_, value) ->
        when (value) {
            is Boolean -> JsonPrimitive(value)
            is Float -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Long -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is Set<*> -> JsonArray(value.filterIsInstance<String>().map { JsonPrimitive(it) })
            null -> JsonNull
            else -> throw IllegalArgumentException("Unsupported type for serialization: ${value::class.java}")
        }
    }
}


fun Context.exportSettings() {
    val settingsPrefs = getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
    val globalFxPrefs = getSharedPreferences(GLOBAL_FX, Context.MODE_PRIVATE)

    val settingsJson = settingsPrefs.all.toJsonElementMap()
    val globalFxJson = globalFxPrefs.all.toJsonElementMap()
    val customFxJson = globalFxPrefs.getStringSet(CUSTOM_EFFECTS, emptySet())?.map { fxName ->
        fxName to getSharedPreferences("fx_$fxName", Context.MODE_PRIVATE).all.toJsonElementMap()
    }

    val allPrefsJson = mutableMapOf(SETTINGS_NAME to settingsJson, GLOBAL_FX to globalFxJson)
    customFxJson?.forEach { (name, map) -> allPrefsJson["fx_$name"] = map }

    val downloadsDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
    val settingsDir = File(downloadsDir, "Echo/Settings/")
    if (!settingsDir.exists()) settingsDir.mkdirs()
    val datefmt = SimpleDateFormat("yyyyMMdd_HHmmss", resources.configuration.locales[0])
    val timestamp = datefmt.format(Date())
    val fileName = "Echo_$timestamp.json"
    val file = File(settingsDir, fileName)
    if (file.exists()) file.delete()

    val jsonString = Json.encodeToString(allPrefsJson)
    FileWriter(file).use { writer -> writer.write(jsonString) }

    val msg = getString(R.string.exported_to, file.absolutePath)
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}


fun Context.importSettings(uri: Uri) {
    val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
        BufferedReader(InputStreamReader(inputStream)).readText()
    } ?: return

    val allPrefsJson = Json.decodeFromString<Map<String, JsonObject>>(jsonString)

    allPrefsJson.forEach { (prefName, prefMap) ->
        getSharedPreferences(prefName, Context.MODE_PRIVATE).edit {
            prefMap.forEach { (key, value) ->
                when (value) {
                    is JsonPrimitive if value.booleanOrNull != null -> putBoolean(
                        key,
                        value.booleanOrNull!!
                    )

                    is JsonPrimitive if value.intOrNull != null -> putInt(key, value.intOrNull!!)
                    is JsonPrimitive if value.longOrNull != null -> putLong(key, value.longOrNull!!)
                    is JsonPrimitive if value.floatOrNull != null -> putFloat(
                        key,
                        value.floatOrNull!!
                    )

                    is JsonPrimitive if value.isString -> putString(key, value.content)
                    is JsonArray -> putStringSet(
                        key,
                        value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.toSet()
                    )

                    is JsonNull -> remove(key)
                    else -> throw IllegalArgumentException("Unsupported type for deserialization: ${value::class.java}")
                }
            }
        }
    }
}


fun Context.exportExtensionSettings(extensionType: String, extensionId: String) {
    val prefName = extensionPrefId(extensionType, extensionId)
    val settingsPrefs = getSharedPreferences(prefName, Context.MODE_PRIVATE)
    val settingsJson = settingsPrefs.all.toJsonElementMap()

    val downloadsDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
    val settingsDir = File(downloadsDir, "Echo/Settings/")
    if (!settingsDir.exists()) settingsDir.mkdirs()
    val datefmt = SimpleDateFormat("yyyyMMdd_HHmmss", resources.configuration.locales[0])
    val timestamp = datefmt.format(Date())
    val fileName = "${extensionId}_$timestamp.json"
    val file = File(settingsDir, fileName)
    if (file.exists()) file.delete()

    val jsonString = Json.encodeToString(settingsJson)
    FileWriter(file).use { writer -> writer.write(jsonString) }

    val msg = getString(R.string.exported_to, file.absolutePath)
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}


fun Context.importExtensionSettings(extensionType: String, extensionId: String, uri: Uri) {
    val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
        BufferedReader(InputStreamReader(inputStream)).readText()
    } ?: return

    val settingsJson = Json.decodeFromString<Map<String, JsonElement>>(jsonString)
    val prefName = extensionPrefId(extensionType, extensionId)

    getSharedPreferences(prefName, Context.MODE_PRIVATE).edit {
        settingsJson.forEach { (key, value) ->
            when (value) {
                is JsonPrimitive if value.booleanOrNull != null -> putBoolean(
                    key,
                    value.booleanOrNull!!
                )

                is JsonPrimitive if value.intOrNull != null -> putInt(key, value.intOrNull!!)
                is JsonPrimitive if value.longOrNull != null -> putLong(key, value.longOrNull!!)
                is JsonPrimitive if value.floatOrNull != null -> putFloat(
                    key,
                    value.floatOrNull!!
                )

                is JsonPrimitive if value.isString -> putString(key, value.content)
                is JsonArray -> putStringSet(
                    key,
                    value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.toSet()
                )

                is JsonNull -> remove(key)
                else -> throw IllegalArgumentException("Unsupported type for deserialization: ${value::class.java}")
            }
        }
    }
}