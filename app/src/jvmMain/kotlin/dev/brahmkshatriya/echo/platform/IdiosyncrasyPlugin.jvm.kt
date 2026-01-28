package dev.brahmkshatriya.echo.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastRoundToInt
import com.skydoves.landscapist.plugins.ImagePlugin
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.MipmapMode

actual class IdiosyncrasyPlugin : ImagePlugin.PainterPlugin {
    @Composable
    override fun compose(
        imageBitmap: ImageBitmap,
        painter: Painter
    ): Painter {
        return remember(imageBitmap) { ScaledBitmapPainter(imageBitmap) }
    }
}

class ScaledBitmapPainter(
    val image: ImageBitmap,
    val filterQuality: FilterQuality = FilterQuality.High
) : Painter() {
    override val intrinsicSize: Size
        get() = Size(image.width.toFloat(), image.height.toFloat())

    override fun DrawScope.onDraw() {
        val size = IntSize(
            size.width.fastRoundToInt(),
            size.height.fastRoundToInt(),
        )
        val bitmap = Image.makeFromBitmap(image.asSkiaBitmap())
            .scale(size.width, size.height)
        drawImage(
            bitmap,
            IntOffset.Zero,
            size,
            dstSize = size,
            alpha = 1.0f,
            colorFilter = null,
            filterQuality = filterQuality,
        )
    }

    fun Image.scale(width: Int, height: Int): ImageBitmap {
        val bitmap = Bitmap()
        bitmap.allocN32Pixels(width, height)
        scalePixels(
            bitmap.peekPixels()!!,
            FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR),
            false
        )
        return Image.makeFromBitmap(bitmap).toComposeImageBitmap()
    }
}