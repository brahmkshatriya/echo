package dev.brahmkshatriya.echo.utils

import androidx.room.TypeConverter

class MapTypeConverter {
    @TypeConverter
    fun fromString(value: String): Map<String, String> = value.split(",,").mapNotNull {
            if (it.isEmpty()) return@mapNotNull null
            val (k, v) = it.split("::")
            k.unEscape() to v.unEscape()
        }.toMap()

    @TypeConverter
    fun fromMap(map: Map<String, String>) =
        map.map { "${it.key.escape()}::${it.value.escape()}" }.joinToString(",,")

    private fun String.escape() = replace(":", "\\:")
        .replace(",", "\\,")

    private fun String.unEscape() = replace("\\:", ":")
        .replace("\\,", ",")
}