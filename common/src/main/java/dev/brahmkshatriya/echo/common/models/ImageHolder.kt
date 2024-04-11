package dev.brahmkshatriya.echo.common.models

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import dev.brahmkshatriya.echo.common.models.UrlHolder.Companion.toUrlHolder
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class ImageHolder : Parcelable {
    abstract val crop: Boolean

    data class UrlImageHolder(val urlHolder: UrlHolder, override val crop: Boolean) : ImageHolder()
    data class UriImageHolder(val uri: Uri, override val crop: Boolean) : ImageHolder()
    data class BitmapImageHolder(val bitmap: Bitmap, override val crop: Boolean) : ImageHolder()

    companion object {
        fun Uri.toImageHolder(crop: Boolean = false): UriImageHolder {
            return UriImageHolder(this, crop)
        }

        fun String.toImageHolder(headers: Map<String, String> = mapOf(), crop: Boolean = false): UrlImageHolder {
            return UrlImageHolder(this.toUrlHolder(headers), crop)
        }

        fun Bitmap.toImageHolder(crop: Boolean = false): BitmapImageHolder {
            return BitmapImageHolder(this, crop)
        }
    }
}

