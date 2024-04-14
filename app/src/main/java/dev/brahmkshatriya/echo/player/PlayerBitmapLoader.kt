package dev.brahmkshatriya.echo.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.ListenableFuture
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.utils.loadBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future

@UnstableApi
class PlayerBitmapLoader(
    val context: Context,
    private val global: Queue,
    private val scope: CoroutineScope
) : BitmapLoader {

    override fun supportsMimeType(mimeType: String) = true

    override fun decodeBitmap(data: ByteArray) = scope.future(Dispatchers.IO) {
        BitmapFactory.decodeByteArray(data, 0, data.size) ?: error("Could not decode image data")
    }

    private val emptyBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.art_music)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = scope.future(Dispatchers.IO) {
        val track = global.getTrack(uri.toString())?.run {
            loaded?: unloaded
        }
        track?.cover?.loadBitmap(context) ?: emptyBitmap
    }
}