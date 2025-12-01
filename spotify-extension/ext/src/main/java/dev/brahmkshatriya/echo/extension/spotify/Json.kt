package dev.brahmkshatriya.echo.extension.spotify

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Json {
    val parser = Json {
        ignoreUnknownKeys = true
    }

    inline fun <reified T> encode(data: T) = parser.encodeToString(data)
    inline fun <reified T> decode(data: String) =
        runCatching { parser.decodeFromString<T>(data) }
            .getOrElse { throw DecodeException(data, it) }

    class DecodeException(data: String, cause: Throwable) : Exception(cause) {
        override val message = "${cause.message}\n$data"
    }
}