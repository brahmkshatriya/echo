package dev.brahmkshatriya.echo.db.models

import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.Request

data class ImageHolderEntity(
    val url: String?,
    val headers: Map<String, String> = mapOf(),
    val crop: Boolean = false,
) {
    companion object {
        fun ImageHolder.toEntity() = when (this) {
            is ImageHolder.UrlRequestImageHolder ->
                ImageHolderEntity(request.url, request.headers, crop)

            else -> null
        }

        fun ImageHolderEntity.toImageHolder() =
            ImageHolder.UrlRequestImageHolder(Request(url ?: "", headers), crop)
    }
}