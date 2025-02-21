package dev.brahmkshatriya.echo.utils.image

import android.graphics.Bitmap
import coil3.size.Size
import coil3.transform.Transformation

class SquareCropTransformation : Transformation() {
    val version = 1
    override val cacheKey = "${javaClass.simpleName}.$version"
    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val max = input.width.coerceAtMost(input.height)
        val x = (input.width - max) / 2
        val y = (input.height - max) / 2
        return Bitmap.createBitmap(input, x, y, max, max)
    }
}