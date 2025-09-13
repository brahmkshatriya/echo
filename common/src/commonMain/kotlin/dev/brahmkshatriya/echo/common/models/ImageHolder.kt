package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.NetworkRequest.Companion.toGetRequest
import kotlinx.serialization.Serializable

/**
 * A class representing an image.
 *
 * Use [toImageHolder] to convert a string to a [NetworkRequestImageHolder].
 *
 * @see [NetworkRequestImageHolder]
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
    data class NetworkRequestImageHolder(val request: NetworkRequest, override val crop: Boolean) :
        ImageHolder()

    /**
     * A data class representing an image from a Resource URI
     *
     * @param uri The URI of the image
     * @param crop Whether to crop the image
     */
    @Serializable
    data class ResourceUriImageHolder(val uri: String, override val crop: Boolean) : ImageHolder()

    /**
     * A data class representing an image from a resource
     *
     * @param resId The resource ID of the image
     * @param crop Whether to crop the image
     */
    @Serializable
    data class ResourceIdImageHolder(val resId: Int, override val crop: Boolean) : ImageHolder()

    /**
     * A data class representing an image from a hex color
     * supported format: #RRGGBB or #AARRGGBB
     *
     * @param hex The hex color code of the image
     * @param crop Whether to crop the image
     */
    @Serializable
    data class HexColorImageHolder(val hex: String) : ImageHolder() {
        init {
            require(hexPattern.matches(hex)) {
                "Invalid hex color format: $hex. Use #RRGGBB or #AARRGGBB."
            }
        }

        override val crop = false
    }

    companion object {
        private val hexPattern = Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$")

        /**
         * Converts a string to a [NetworkRequestImageHolder]
         *
         * @param headers The headers to be sent with the request
         * @param crop Whether to crop the image
         */
        fun String.toImageHolder(
            headers: Map<String, String> = mapOf(),
            crop: Boolean = false
        ) = NetworkRequestImageHolder(this.toGetRequest(headers), crop)

        /**
         * Converts a string to a [ResourceUriImageHolder]
         *
         * @param crop Whether to crop the image
         */
        fun String.toResourceUriImageHolder(crop: Boolean = false) = ResourceUriImageHolder(this, crop)

        /**
         * Converts an integer to a [ResourceIdImageHolder]
         *
         * @param crop Whether to crop the image
         */
        fun Int.toResourceImageHolder(crop: Boolean = false) = ResourceIdImageHolder(this, crop)

        /**
         * Converts a hex color code to a [HexColorImageHolder]
         *
         * @param crop Whether to crop the image
         */
        fun String.toHexColorImageHolder() = HexColorImageHolder(this)
    }
}