package dev.brahmkshatriya.echo.common.models

import kotlinx.serialization.Serializable

@Serializable
data class Request(
    val url: String,
    val headers: Map<String, String> = emptyMap()
) {
    companion object {
        fun String.toRequest(headers: Map<String, String> = emptyMap()): Request {
            return Request(this, headers)
        }
    }
}