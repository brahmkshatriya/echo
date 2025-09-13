package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.models.NetworkRequest.Companion.toGetRequest
import dev.brahmkshatriya.echo.common.models.NetworkRequest.Method.CONNECT
import dev.brahmkshatriya.echo.common.models.NetworkRequest.Method.DELETE
import dev.brahmkshatriya.echo.common.models.NetworkRequest.Method.GET
import dev.brahmkshatriya.echo.common.models.NetworkRequest.Method.HEAD
import dev.brahmkshatriya.echo.common.models.NetworkRequest.Method.OPTIONS
import dev.brahmkshatriya.echo.common.models.NetworkRequest.Method.PATCH
import dev.brahmkshatriya.echo.common.models.NetworkRequest.Method.POST
import dev.brahmkshatriya.echo.common.models.NetworkRequest.Method.PUT
import dev.brahmkshatriya.echo.common.models.NetworkRequest.Method.TRACE
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * A data class to represent a network request.
 *
 * @param url The URL to make the request to
 * @param headers The headers to be sent with the request
 * @param method The HTTP method to use for the request, defaults to [Method.GET]
 * @param bodyBase64 The body of the request encoded in Base64, can be null
 *
 * @see [toGetRequest]
 */
@OptIn(ExperimentalEncodingApi::class)
@Serializable
data class NetworkRequest(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val method: Method = GET,
    val bodyBase64: String? = null,
) {

    /**
     * Represents the HTTP methods that can be used in a network request.
     * - [GET] for retrieving data
     * - [POST] for sending data
     * - [PUT] for updating data
     * - [DELETE] for deleting data
     * - [PATCH] for partially updating data
     * - [HEAD] for retrieving the headers of a resource without the body
     * - [OPTIONS] for requesting information about the communication options available
     * - [TRACE] for tracing the path to the resource
     * - [CONNECT] for establishing a tunnel to the server
     */
    enum class Method {
        GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE, CONNECT
    }

    /**
     * The body of the request decoded from Base64.
     * If [bodyBase64] is null, this will also be null.
     *
     * @return The decoded body as a [ByteArray] or null if [bodyBase64] is null.
     */
    val body by lazy {
        bodyBase64?.let { Base64.decode(it) }
    }

    /**
     * A map of headers with all keys converted to lowercase.
     * This is useful for case-insensitive header lookups.
     */
    val lowerCaseHeaders by lazy {
        headers.mapKeys { it.key.lowercase() }
    }

    constructor(
        method: Method,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: ByteArray? = null,
    ) : this(
        url = url,
        headers = headers,
        method = method,
        bodyBase64 = body?.let { Base64.encode(it) }
    )

    companion object {

        /**
         * Converts the string to a [NetworkRequest] object
         *
         * @param headers The headers to be sent with the request
         *
         * @return A [NetworkRequest] object
         */
        fun String.toGetRequest(headers: Map<String, String> = emptyMap()): NetworkRequest {
            return NetworkRequest(this, headers)
        }
    }
}