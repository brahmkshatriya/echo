package dev.brahmkshatriya.echo.player

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import dev.brahmkshatriya.echo.data.models.ImageHolder
import dev.brahmkshatriya.echo.data.models.StreamableAudio
import dev.brahmkshatriya.echo.data.models.Track
import dev.brahmkshatriya.echo.player.PlayerListener.Companion.tracks
import java.nio.ByteBuffer


interface PlayerHelper {
    companion object {

        fun mediaItemBuilder(track:Track, audio: StreamableAudio): MediaItem {
            val builder = MediaItem.Builder()

            val item = when (audio) {
                is StreamableAudio.StreamableFile -> {
                    builder.setUri(audio.uri)
                }

                is StreamableAudio.StreamableUrl -> {
                    builder.setUri(audio.url.url)
                }

                is StreamableAudio.ByteStreamAudio -> TODO()
            }

            val metadata = track.toMetaData()
            item.setMediaMetadata(metadata)
            item.setMediaId(track.uri.toString())
            item.setTag(track)
            tracks[track.uri.toString()] = track
            return item.build()
        }

        @SuppressLint("UnsafeOptInUsageError")
        fun Track.toMetaData() = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artists.firstOrNull()?.name)
            .setArtwork(cover)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .build()

        @UnstableApi
        private fun MediaMetadata.Builder.setArtwork(cover: ImageHolder?): MediaMetadata.Builder {
            cover?.let {
                return when (it) {
                    is ImageHolder.UrlHolder -> setArtworkUri(it.url.toUri())
                    is ImageHolder.UriHolder -> setArtworkUri(it.uri)
                    is ImageHolder.BitmapHolder -> setArtworkData(
                        it.bitmap.toByteArray(),
                        MediaMetadata.PICTURE_TYPE_FILE_ICON
                    )
                }
            }
            return this
        }

        private fun Bitmap.toByteArray(): ByteArray {
            val size: Int = rowBytes * height
            val byteBuffer = ByteBuffer.allocate(size)
            copyPixelsToBuffer(byteBuffer)
            return byteBuffer.array()
        }


        fun Long.toTimeString(): String {
            val seconds = this / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
            } else {
                String.format("%02d:%02d", minutes, seconds % 60)
            }
        }


    }
}

