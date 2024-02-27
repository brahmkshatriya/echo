package dev.brahmkshatriya.echo.common.models

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize


sealed class ImageHolder : Parcelable {
    @Parcelize
    data class UrlHolder(val url: String, val headers: Map<String, String>) : ImageHolder()
    @Parcelize
    data class UriHolder(val uri: Uri) : ImageHolder()
    @Parcelize
    data class BitmapHolder(val bitmap: Bitmap) : ImageHolder()

    companion object {
        fun Uri.toImageHolder(): UriHolder {
            return UriHolder(this)
        }

        fun String.toImageHolder(headers: Map<String, String>? = null): UrlHolder {
            return UrlHolder(this, headers ?: mapOf())
        }

        fun Bitmap.toImageHolder(): BitmapHolder {
            return BitmapHolder(this)
        }
    }
}

