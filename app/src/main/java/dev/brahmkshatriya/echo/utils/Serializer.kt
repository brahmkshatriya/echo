package dev.brahmkshatriya.echo.utils

import android.os.Bundle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

inline fun <reified T> String.toData() = json.decodeFromString<T>(this)
inline fun <reified T> T.toJson() = json.encodeToString(this)

inline fun <reified T> Bundle.putSerialized(key: String, value: T) {
    putString(key, value.toJson())
}

inline fun <reified T> Bundle.getSerialized(key: String): T? {
    return getString(key)?.toData()
}