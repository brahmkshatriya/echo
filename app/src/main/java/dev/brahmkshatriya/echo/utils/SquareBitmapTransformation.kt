package dev.brahmkshatriya.echo.utils

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

class SquareBitmapTransformation : BitmapTransformation() {

    val version = 1
    val id = "${javaClass.simpleName}.$version".toByteArray()
    override fun updateDiskCacheKey(messageDigest: MessageDigest) = messageDigest.update(id)

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val size = toTransform.width.coerceAtMost(toTransform.height)
        val x = (toTransform.width - size) / 2
        val y = (toTransform.height - size) / 2
        return Bitmap.createBitmap(toTransform, x, y, size, size)
    }
}