package dev.brahmkshatriya.echo.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.ListenableFuture
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.playback.DelayedSource.Companion.getMediaItemById
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.utils.loadBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.withContext

@UnstableApi
class PlayerBitmapLoader(
    val context: Context,
    val player: Player,
    private val scope: CoroutineScope
) : BitmapLoader {

    override fun supportsMimeType(mimeType: String) = true

    override fun decodeBitmap(data: ByteArray) = scope.future(Dispatchers.IO) {
        BitmapFactory.decodeByteArray(data, 0, data.size) ?: error("Failed to decode bitmap")
    }

    private val emptyBitmap
        get() = context.loadBitmap(R.drawable.art_music) ?: error("Empty bitmap")

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = scope.future(Dispatchers.IO) {
        val (_, mediaItem) = withContext(Dispatchers.Main) {
            player.getMediaItemById(uri.toString())
        } ?: return@future emptyBitmap
        mediaItem.track.cover?.loadBitmap(context) ?: emptyBitmap
    }
}