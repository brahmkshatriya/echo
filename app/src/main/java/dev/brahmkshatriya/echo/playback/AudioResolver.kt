package dev.brahmkshatriya.echo.playback

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource.Resolver
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.playback.DelayedSource.Companion.getMediaItemById
import dev.brahmkshatriya.echo.playback.DelayedSource.Companion.getTrackClient
import dev.brahmkshatriya.echo.playback.MediaItemUtils.audioIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.common.MusicExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

class AudioResolver(
    private val context: Context,
    private val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
) : Resolver {

    lateinit var player: Player

    @UnstableApi
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val id = dataSpec.uri.toString().takeIf { it.startsWith("id://") }
            ?.substringAfter("id://") ?: return dataSpec

        val (_, mediaItem) = runBlocking(Dispatchers.Main) {
            player.getMediaItemById(id)
        } ?: throw Exception(context.getString(R.string.track_not_found))

        val streamableAudio =
            runBlocking(Dispatchers.IO) { runCatching { loadAudio(mediaItem) } }.getOrThrow()
        return dataSpec.copy(
            uri = streamableAudio.hashCode().toString().toUri(),
            customData = streamableAudio
        )
    }

    private suspend fun loadAudio(mediaItem: MediaItem): Streamable.Audio {
        val streams = mediaItem.track.audioStreamables
        val index = mediaItem.audioIndex
        val streamable = streams[index]
        return mediaItem.getTrackClient(context, extensionListFlow) {
            when (val media = getStreamableMedia(streamable)) {
                is Streamable.Media.AudioOnly -> media.audio
                is Streamable.Media.WithVideo.WithAudio -> media.toAudio()
                else -> throw IllegalStateException()
            }
        }
    }

    companion object {
        @OptIn(UnstableApi::class)
        fun DataSpec.copy(
            uri: Uri? = null,
            uriPositionOffset: Long? = null,
            httpMethod: Int? = null,
            httpBody: ByteArray? = null,
            httpRequestHeaders: Map<String, String>? = null,
            position: Long? = null,
            length: Long? = null,
            key: String? = null,
            flags: Int? = null,
            customData: Any? = null
        ): DataSpec {
            return DataSpec.Builder()
                .setUri(uri ?: this.uri)
                .setUriPositionOffset(uriPositionOffset ?: this.uriPositionOffset)
                .setHttpMethod(httpMethod ?: this.httpMethod)
                .setHttpBody(httpBody ?: this.httpBody)
                .setHttpRequestHeaders(httpRequestHeaders ?: this.httpRequestHeaders)
                .setPosition(position ?: this.position)
                .setLength(length ?: this.length)
                .setKey(key ?: this.key)
                .setFlags(flags ?: this.flags)
                .setCustomData(customData ?: this.customData)
                .build()
        }

    }
}