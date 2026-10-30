package dev.brahmkshatriya.echo.app.platform

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import kotlin.math.roundToInt

actual fun ImageBitmap.toByteArray(quality: Int, maxSize: Int): ByteArray {
    require(quality in -1..100) { "Image quality must be -1 or between 0 and 100" }
    require(maxSize > 0) { "Image size must be positive" }
    val format = if (quality == -1) EncodedImageFormat.PNG else EncodedImageFormat.JPEG
    val encodingQuality = if (quality == -1) 100 else quality
    val scale = (maxSize.toFloat() / maxOf(width, height)).coerceAtMost(1f)
    val thumbnail = if (scale == 1f) this else {
        val size = IntSize((width * scale).roundToInt().coerceAtLeast(1), (height * scale).roundToInt().coerceAtLeast(1))
        ImageBitmap(size.width, size.height).also {
            Canvas(it).drawImageRect(this, IntOffset.Zero, IntSize(width, height), IntOffset.Zero, size, Paint())
        }
    }
    try {
        val image = Image.makeFromBitmap(thumbnail.asSkiaBitmap())
        try {
            val encoded = checkNotNull(image.encodeToData(format, encodingQuality)) { "Could not encode image" }
            try {
                return encoded.bytes
            } finally {
                encoded.close()
            }
        } finally {
            image.close()
        }
    } finally {
        if (thumbnail !== this) thumbnail.asSkiaBitmap().close()
    }
}
