package dev.brahmkshatriya.echo.playback

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.ThumbRating
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.selectAudioIndex
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.selectVideoIndex
import dev.brahmkshatriya.echo.utils.getSerialized
import dev.brahmkshatriya.echo.utils.putSerialized
import dev.brahmkshatriya.echo.utils.toData
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
        val metadata = track.toMetaData(bundleOf(), clientId, context, false, settings)
        item.setMediaMetadata(metadata)
        item.setMediaId(track.id)
        return item.build()
    }

    fun build(
        settings: SharedPreferences?,
        mediaItem: MediaItem,
        track: Track
    ): MediaItem = with(mediaItem) {
        val item = buildUpon()
        val metadata =
            track.toMetaData(mediaMetadata.extras!!, clientId, context, true, settings)
        item.setMediaMetadata(metadata)
        return item.build()
    }

    fun buildAudio(mediaItem: MediaItem, index: Int): MediaItem = with(mediaItem) {
        val bundle = Bundle().apply {
            putAll(mediaMetadata.extras!!)
            putInt("audioStream", index)
        }
        build(this, bundle)
    }

    fun buildVideo(mediaItem: MediaItem, index: Int): MediaItem = with(mediaItem) {
        val bundle = Bundle().apply {
            putAll(mediaMetadata.extras!!)
            putInt("videoStream", index)
        }
        build(this, bundle)
    }

    fun buildSubtitle(mediaItem: MediaItem, index: Int): MediaItem = with(mediaItem) {
        val bundle = Bundle().apply {
            putAll(mediaMetadata.extras!!)
            putInt("subtitle", index)
        }
        build(this, bundle)
    }

    fun build(mediaItem: MediaItem, isVideo: Boolean) = with(mediaItem) {
        val item = buildUpon()
        val data = Pair(mediaId, isVideo).toJson()
        item.setUri(data)
        item.build()
    }

    fun Uri.toIdAndIsVideo() = runCatching {
        val string = toString()
        if (string.startsWith('{')) string.toData<Pair<String, Boolean>>()
        else null
    }.getOrNull()

    fun build(mediaItem: MediaItem, bundle: Bundle) = run {
        val item = mediaItem.buildUpon()
        val metadata = mediaItem.mediaMetadata.buildUpon()
            .setExtras(bundle)
            .setSubtitle(bundle.indexes())
            .build()
        item.setMediaMetadata(metadata)
        item.build()
    }

    fun build(
        mediaItem: MediaItem,
        video: Streamable.Media.WithVideo?,
        subtitle: Streamable.Media.Subtitle?
    ) = with(mediaItem) {
        val bundle = mediaMetadata.extras!!
        bundle.putSerialized("video", video)
        val item = buildUpon()
        item.setSubtitleConfigurations(
            if (subtitle == null) listOf()
            else listOf(
                MediaItem.SubtitleConfiguration.Builder(subtitle.url.toUri())
                    .setMimeType(subtitle.type.toMimeType())
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
            )
        )
        item.build()
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
        subtitleIndex: Int? = null
    ) = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artists.joinToString(", ") { it.name })
        .setArtworkUri(cover?.toJson()?.toUri())
        .setUserRating(ThumbRating(isLiked))
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
                putInt(
                    "subtitle",
                    subtitleIndex ?: 0.takeIf { subtitleStreamables.isNotEmpty() } ?: -1
                )
            }
        )
        .setSubtitle(bundle.indexes())
        .setIsPlayable(loaded)
        .build()


    private fun Bundle.indexes() =
        "${getInt("audioStream")} ${getInt("videoStream")} ${getInt("subtitle")}"

    val MediaMetadata.isLoaded get() = extras?.getBoolean("loaded") ?: false
    val MediaMetadata.track get() = requireNotNull(extras?.getSerialized<Track>("track"))
    val MediaMetadata.clientId get() = requireNotNull(extras?.getString("clientId"))
    val MediaMetadata.context get() = extras?.getSerialized<EchoMediaItem?>("context")
    val MediaMetadata.audioIndex get() = extras?.getInt("audioStream") ?: -1
    val MediaMetadata.videoIndex get() = extras?.getInt("videoStream") ?: -1
    val MediaMetadata.subtitleIndex get() = extras?.getInt("subtitle") ?: -1
    val MediaMetadata.isLiked get() = (userRating as? ThumbRating)?.isThumbsUp == true
    val MediaMetadata.video get() = extras?.getSerialized<Streamable.Media.WithVideo?>("video")

    val MediaItem.track get() = mediaMetadata.track
    val MediaItem.clientId get() = mediaMetadata.clientId
    val MediaItem.context get() = mediaMetadata.context
    val MediaItem.isLoaded get() = mediaMetadata.isLoaded
    val MediaItem.audioIndex get() = mediaMetadata.audioIndex
    val MediaItem.videoIndex get() = mediaMetadata.videoIndex
    val MediaItem.subtitleIndex get() = mediaMetadata.subtitleIndex
    val MediaItem.video get() = mediaMetadata.video
    val MediaItem.isLiked get() = mediaMetadata.isLiked

    private fun Streamable.SubtitleType.toMimeType() = when (this) {
        Streamable.SubtitleType.VTT -> MimeTypes.TEXT_VTT
        Streamable.SubtitleType.SRT -> MimeTypes.APPLICATION_SUBRIP
        Streamable.SubtitleType.ASS -> MimeTypes.TEXT_SSA
    }

    private val MediaItem.audioStreamable get() = track.audioStreamables[audioIndex]
    private val MediaItem.videoStreamable get() = track.videoStreamables.getOrNull(videoIndex)
    fun MediaItem.isAudioAndVideoMerged() = when (val video = video) {
        is Streamable.Media.WithVideo.WithAudio -> videoStreamable == audioStreamable
        is Streamable.Media.WithVideo.Only -> if (!video.looping) false else null
        null -> null
    }

    fun MediaItem.getStreamable(isVideo: Boolean) =
        if (isVideo) track.videoStreamables[videoIndex]
        else track.audioStreamables[audioIndex]

}
