package dev.brahmkshatriya.echo.data.models

import android.graphics.Bitmap
import android.net.Uri

sealed class ImageHolder {
    data class UrlHolder(val url: String, val headers: Map<String, String>) : ImageHolder()
    data class UriHolder(val uri: Uri) : ImageHolder()
    data class BitmapHolder(val bitmap: Bitmap) : ImageHolder()
}

fun Uri.toImageHolder(): ImageHolder.UriHolder {
    return ImageHolder.UriHolder(this)
}

fun String.toImageHolder(headers: Map<String, String>? = null): ImageHolder.UrlHolder {
    return ImageHolder.UrlHolder(this, headers ?: mapOf())
}

fun Bitmap.toImageHolder(): ImageHolder.BitmapHolder {
    return ImageHolder.BitmapHolder(this)
}