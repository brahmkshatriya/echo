package dev.brahmkshatriya.echo.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import com.skydoves.landscapist.plugins.ImagePlugin

actual class IdiosyncrasyPlugin : ImagePlugin.PainterPlugin{
    @Composable
    override fun compose(imageBitmap: ImageBitmap, painter: Painter) = painter
}