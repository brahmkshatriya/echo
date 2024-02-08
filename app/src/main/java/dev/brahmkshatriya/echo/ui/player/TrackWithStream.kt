package dev.brahmkshatriya.echo.ui.player

import androidx.media3.common.MediaItem
import dev.brahmkshatriya.echo.data.models.StreamableAudio
import dev.brahmkshatriya.echo.data.models.Track
import dev.brahmkshatriya.echo.ui.player.PlayerHelper.Companion.toMetaData

class TrackWithStream(
    val track: Track,
    private val audio: StreamableAudio
) {
    fun mediaItemBuilder(): MediaItem {
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
        item.setTag(track)

        return item.build()
    }
}