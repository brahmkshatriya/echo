package dev.brahmkshatriya.echo.common.models

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class ImageHolder : Parcelable {
    abstract val crop: Boolean

    data class UrlHolder(
        val url: String, val headers: Map<String, String>,
        override val crop: Boolean
    ) : ImageHolder()

    data class UriHolder(val uri: Uri, override val crop: Boolean) : ImageHolder()
    data class BitmapHolder(val bitmap: Bitmap, override val crop: Boolean) : ImageHolder()

    companion object {
        fun Uri.toImageHolder(crop: Boolean = false): UriHolder {
            return UriHolder(this, crop)
        }

        fun String.toImageHolder(headers: Map<String, String> = mapOf(), crop: Boolean = false): UrlHolder {
            return UrlHolder(this, headers, crop)
        }

        fun Bitmap.toImageHolder(crop: Boolean = false): BitmapHolder {
            return BitmapHolder(this, crop)
        }
    }
}

