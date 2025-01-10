package dev.brahmkshatriya.echo.playback.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.ListenableFuture
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.utils.future
import dev.brahmkshatriya.echo.utils.image.loadBitmap
import dev.brahmkshatriya.echo.utils.toData
import kotlinx.coroutines.CoroutineScope

@UnstableApi
class PlayerBitmapLoader(
    val context: Context,
    private val scope: CoroutineScope
) : BitmapLoader {

    override fun supportsMimeType(mimeType: String) = true

    override fun decodeBitmap(data: ByteArray) = scope.future {
        BitmapFactory.decodeByteArray(data, 0, data.size) ?: error("Failed to decode bitmap")
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = scope.future {
        val cover = uri.toString().toData<ImageHolder>()
        cover.loadBitmap(context) ?: error("Failed to load bitmap of $cover")
    }
}