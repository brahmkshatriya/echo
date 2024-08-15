package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.models.Request.Companion.toRequest
import kotlinx.serialization.Serializable
import java.io.Serializable as JSerializable

@Serializable
sealed class ImageHolder : JSerializable {
    abstract val crop: Boolean

    data class UrlRequestImageHolder(val request: Request, override val crop: Boolean) :
        ImageHolder()

    data class UriImageHolder(val uri: String, override val crop: Boolean) : ImageHolder()
    data class BitmapImageHolder(val bitmap: Bitmap, override val crop: Boolean) : ImageHolder()

    companion object {
//        fun Uri.toImageHolder(crop: Boolean = false): UriImageHolder {
//            return UriImageHolder(this, crop)
//        }

        fun String.toImageHolder(
            headers: Map<String, String> = mapOf(),
            crop: Boolean = false
        ): UrlRequestImageHolder {
            return UrlRequestImageHolder(this.toRequest(headers), crop)
        }

        fun Bitmap.toImageHolder(crop: Boolean = false): BitmapImageHolder {
            return BitmapImageHolder(this, crop)
        }
    }
}

