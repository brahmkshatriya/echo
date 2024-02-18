package dev.brahmkshatriya.echo.common.models

import android.graphics.Bitmap
import android.net.Uri

sealed class ImageHolder {
    data class UrlHolder(val url: String, val headers: Map<String, String>) : ImageHolder()
    data class UriHolder(val uri: Uri) : ImageHolder()
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

