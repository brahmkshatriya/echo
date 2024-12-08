package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import kotlinx.serialization.Serializable

/**
 * A data class to represent a network request.
 *
 * @param url The URL to make the request to
 * @param headers The headers to be sent with the request
 *
 * @see [toRequest]
 */
@Serializable
data class Request(
    val url: String,
    val headers: Map<String, String> = emptyMap()
) {
    companion object {

        /**
         * Converts the string to a [Request] object
         *
         * @param headers The headers to be sent with the request
         *
         * @return A [Request] object
         */
        fun String.toRequest(headers: Map<String, String> = emptyMap()): Request {
            return Request(this, headers)
        }
    }
}