package dev.brahmkshatriya.echo.playback

import android.content.SharedPreferences
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.ThumbRating
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.StreamableVideo
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.selectStreamIndex
import dev.brahmkshatriya.echo.utils.getSerialized
import dev.brahmkshatriya.echo.utils.toJson

object MediaItemUtils {

    fun build(
        settings: SharedPreferences?,
        track: Track,
        clientId: String,
        context: EchoMediaItem?,
    ): MediaItem {
        val item = MediaItem.Builder()
        item.setUri(track.id)
        val metadata = track.toMetaData(settings, clientId, context)
        item.setMediaMetadata(metadata)
        item.setMediaId(track.id)
        return item.build()
    }

    fun build(settings: SharedPreferences?, mediaItem: MediaItem, track: Track, video: StreamableVideo?): MediaItem =
        with(mediaItem) {
            val item = buildUpon()
            val metadata = track.toMetaData(settings, clientId, context, true, video)
            item.setMediaMetadata(metadata)
            return item.build()
        }

    private fun Track.toMetaData(
        settings: SharedPreferences?,
        clientId: String,
        context: EchoMediaItem?,
        loaded: Boolean = false,
        video: StreamableVideo? = null,
        audioStreamIndex: Int? = null,
    ) = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artists.joinToString(", ") { it.name })
        .setArtworkUri(id.toUri())
        .setUserRating(ThumbRating(liked))
        .setIsBrowsable(false)
        .setExtras(
            bundleOf(
                "track" to this.toJson(),
                "clientId" to clientId,
                "context" to context.toJson(),
                "loaded" to loaded,
                "audioStream" to selectStream(settings, loaded, audioStreamIndex),
                "videoStream" to null,
                "video" to video?.toJson()
            )
        )

        .setIsPlayable(loaded)
        .build()

    private fun Track.selectStream(
        settings: SharedPreferences?,
        loaded: Boolean,
        audioStreamIndex: Int?
    ): Int? {
        if (!loaded) return null
        if (settings == null) return audioStreamIndex
        return audioStreamIndex ?: selectStreamIndex(settings, audioStreamables)
    }

    val MediaMetadata.isLoaded get() = extras?.getBoolean("loaded") ?: false
    val MediaMetadata.track get() = requireNotNull(extras?.getSerialized<Track>("track"))
    val MediaMetadata.clientId get() = requireNotNull(extras?.getString("clientId"))
    val MediaMetadata.context get() = extras?.getSerialized<EchoMediaItem?>("context")
    val MediaMetadata.audioIndex get() = extras?.getInt("audioStream") ?: -1
    val MediaMetadata.videoIndex get() = extras?.getInt("videoStream") ?: -1
    val MediaMetadata.isLiked get() = (userRating as? ThumbRating)?.isThumbsUp == true
    val MediaMetadata.video get() = extras?.getSerialized<StreamableVideo>("video")

    val MediaItem.track get() = mediaMetadata.track
    val MediaItem.clientId get() = mediaMetadata.clientId
    val MediaItem.context get() = mediaMetadata.context
    val MediaItem.isLoaded get() = mediaMetadata.isLoaded
    val MediaItem.audioIndex get() = mediaMetadata.audioIndex
    val MediaItem.videoIndex get() = mediaMetadata.videoIndex
    val MediaItem.video get() = mediaMetadata.video
    val MediaItem.isLiked get() = mediaMetadata.isLiked

}
