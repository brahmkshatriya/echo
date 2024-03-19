package dev.brahmkshatriya.echo.common.models

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class ImageHolder : Parcelable {
    data class UrlHolder(val url: String, val headers: Map<String, String>) : ImageHolder()
    data class UriHolder(val uri: Uri) : ImageHolder()
    data class BitmapHolder(val bitmap: Bitmap) : ImageHolder()

    companion object {
        fun Uri.toImageHolder(): UriHolder {
            return UriHolder(this)
        }

        fun String.toImageHolder(headers: Map<String, String> = mapOf()): UrlHolder {
            return UrlHolder(this, headers)
        }

        fun Bitmap.toImageHolder(): BitmapHolder {
            return BitmapHolder(this)
        }
    }
}

