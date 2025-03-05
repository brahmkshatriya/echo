package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.ImageHolder.UriImageHolder
import dev.brahmkshatriya.echo.common.models.ImageHolder.UrlRequestImageHolder
import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import kotlinx.serialization.Serializable

/**
 * A class representing an image.
 *
 * Use [toImageHolder] to convert a string to an [UrlRequestImageHolder].
 *
 * @see [UrlRequestImageHolder]
 * @see [UriImageHolder]
 */
@Serializable
sealed class ImageHolder {

    /**
     * Whether to crop the image
     */
    abstract val crop: Boolean

    /**
     * A data class representing an image from a network request
     *
     * @param request The request to fetch the image
     * @param crop Whether to crop the image
     */
    @Serializable
    data class UrlRequestImageHolder(val request: Request, override val crop: Boolean) :
        ImageHolder()

    /**
     * A data class representing an image from a URI
     *
     * @param uri The URI of the image
     * @param crop Whether to crop the image
     */
    @Serializable
    data class UriImageHolder(val uri: String, override val crop: Boolean) : ImageHolder()

    /**
     * A data class representing an image from a resource
     *
     * @param resId The resource ID of the image
     * @param crop Whether to crop the image
     */
    @Serializable
    data class ResourceImageHolder(val resId: Int, override val crop: Boolean) : ImageHolder()

    companion object {

        /**
         * Converts a string to a [UrlRequestImageHolder]
         *
         * @param headers The headers to be sent with the request
         * @param crop Whether to crop the image
         */
        fun String.toImageHolder(
            headers: Map<String, String> = mapOf(),
            crop: Boolean = false
        ) = UrlRequestImageHolder(this.toRequest(headers), crop)

        /**
         * Converts a string to a [UriImageHolder]
         *
         * @param crop Whether to crop the image
         */
        fun String.toUriImageHolder(crop: Boolean = false) = UriImageHolder(this, crop)

        /**
         * Converts an integer to a [ResourceImageHolder]
         *
         * @param crop Whether to crop the image
         */
        fun Int.toResourceImageHolder(crop: Boolean = false) = ResourceImageHolder(this, crop)
    }
}