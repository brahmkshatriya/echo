package dev.brahmkshatriya.echo.playback

import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.ThumbRating
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.selectAudioIndex
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.selectVideoIndex
import dev.brahmkshatriya.echo.utils.getSerialized
import dev.brahmkshatriya.echo.utils.putSerialized

object MediaItemUtils {

    fun build(
        settings: SharedPreferences?,
        track: Track,
        clientId: String,
        context: EchoMediaItem?,
    ): MediaItem {
        val item = MediaItem.Builder()
        item.setUri("id://${track.id}")
        val metadata = track.toMetaData(bundleOf(), clientId, context, false, settings)
        item.setMediaMetadata(metadata)
        item.setMediaId(track.id)
        return item.build()
    }

    fun build(
        settings: SharedPreferences?,
        mediaItem: MediaItem,
        track: Track
    ): MediaItem =
        with(mediaItem) {
            val item = buildUpon()
            val metadata =
                track.toMetaData(mediaMetadata.extras!!, clientId, context, true, settings)
            item.setMediaMetadata(metadata)
            return item.build()
        }

    fun buildAudio(mediaItem: MediaItem, index: Int): MediaItem =
        with(mediaItem) {
            val item = buildUpon()
            val metadata = track.toMetaData(mediaMetadata.extras!!, audioStreamIndex = index)
            item.setMediaMetadata(metadata)
            return item.build()
        }

    fun buildVideo(mediaItem: MediaItem, index: Int): MediaItem =
        with(mediaItem) {
            val item = buildUpon()
            val metadata = track.toMetaData(mediaMetadata.extras!!, videoStreamIndex = index)
            item.setMediaMetadata(metadata)
            return item.build()
        }

    fun build(mediaItem: MediaItem, video: Streamable.Media.WithVideo): MediaItem =
        with(mediaItem) {
            val item = buildUpon()
            val bundle = mediaMetadata.extras!!
            bundle.putSerialized("video", video)
            val metadata = mediaMetadata.buildUpon()
                .setExtras(bundle)
                .build()
            item.setMediaMetadata(metadata)
            return item.build()
        }

    private fun Track.toMetaData(
        bundle: Bundle,
        clientId: String = bundle.getString("clientId")!!,
        context: EchoMediaItem? = bundle.getSerialized("context"),
        loaded: Boolean = bundle.getBoolean("loaded"),
        settings: SharedPreferences? = null,
        video: Streamable.Media.WithVideo? = null,
        audioStreamIndex: Int? = null,
        videoStreamIndex: Int? = null,
    ) = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artists.joinToString(", ") { it.name })
        .setArtworkUri(id.toUri())
        .setUserRating(ThumbRating(liked))
        .setIsBrowsable(false)
        .setExtras(
            bundle.apply {
                putSerialized("track", this@toMetaData)
                putString("clientId", clientId)
                putSerialized("context", context)
                putBoolean("loaded", loaded)
                putSerialized("video", video)
                putInt(
                    "audioStream",
                    audioStreamIndex ?: selectAudioIndex(settings, audioStreamables)
                )
                putInt(
                    "videoStream",
                    videoStreamIndex ?: selectVideoIndex(settings, videoStreamables)
                )
            }
        )

        .setIsPlayable(loaded)
        .build()

    val MediaMetadata.isLoaded get() = extras?.getBoolean("loaded") ?: false
    val MediaMetadata.track get() = requireNotNull(extras?.getSerialized<Track>("track"))
    val MediaMetadata.clientId get() = requireNotNull(extras?.getString("clientId"))
    val MediaMetadata.context get() = extras?.getSerialized<EchoMediaItem?>("context")
    val MediaMetadata.audioIndex get() = extras?.getInt("audioStream") ?: -1
    val MediaMetadata.videoIndex get() = extras?.getInt("videoStream") ?: -1
    val MediaMetadata.isLiked get() = (userRating as? ThumbRating)?.isThumbsUp == true
    val MediaMetadata.video get() = extras?.getSerialized<Streamable.Media.WithVideo?>("video")

    val MediaItem.track get() = mediaMetadata.track
    val MediaItem.clientId get() = mediaMetadata.clientId
    val MediaItem.context get() = mediaMetadata.context
    val MediaItem.isLoaded get() = mediaMetadata.isLoaded
    val MediaItem.audioIndex get() = mediaMetadata.audioIndex
    val MediaItem.videoIndex get() = mediaMetadata.videoIndex
    val MediaItem.video get() = mediaMetadata.video
    val MediaItem.isLiked get() = mediaMetadata.isLiked

    val MediaItem.audioStreamable get() = track.audioStreamables[audioIndex]
    val MediaItem.videoStreamable get() = track.videoStreamables.getOrNull(videoIndex)
}
