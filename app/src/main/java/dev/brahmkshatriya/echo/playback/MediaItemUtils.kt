package dev.brahmkshatriya.echo.playback

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.ThumbRating
import androidx.media3.common.util.UnstableApi
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.selectSourceIndex
import dev.brahmkshatriya.echo.utils.Delegated.Companion.delegated
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import dev.brahmkshatriya.echo.utils.Serializer.toData
import dev.brahmkshatriya.echo.utils.Serializer.toJson

object MediaItemUtils {

    fun build(
        settings: SharedPreferences?,
        track: Track,
        clientId: String,
        context: EchoMediaItem?,
    ): MediaItem {
        val item = MediaItem.Builder()
        val metadata = track.toMetaData(bundleOf(), clientId, context, false, settings)
        item.setMediaMetadata(metadata)
        item.setMediaId(track.id)
        item.setUri(track.id)
        return item.build()
    }

    fun buildLoaded(
        settings: SharedPreferences?, mediaItem: MediaItem, track: Track
    ): MediaItem = with(mediaItem) {
        val item = buildUpon()
        val metadata =
            track.toMetaData(mediaMetadata.extras!!, extensionId, context, true, settings)
        item.setMediaMetadata(metadata)
        return item.build()
    }

    fun buildServer(mediaItem: MediaItem, index: Int): MediaItem = with(mediaItem) {
        val bundle = Bundle().apply {
            putAll(mediaMetadata.extras!!)
            putInt("serverIndex", index)
            putInt("retries", 0)
        }
        buildWithBundle(this, bundle)
    }

    fun buildSource(mediaItem: MediaItem, index: Int) = with(mediaItem) {
        val bundle = Bundle().apply {
            putAll(mediaMetadata.extras!!)
            putInt("sourceIndex", index)
            putInt("retries", 0)
        }
        buildWithBundle(this, bundle)
    }

    fun buildBackground(mediaItem: MediaItem, index: Int): MediaItem = with(mediaItem) {
        val bundle = Bundle().apply {
            putAll(mediaMetadata.extras!!)
            putInt("backgroundIndex", index)
        }
        buildWithBundle(this, bundle)
    }

    fun buildSubtitle(mediaItem: MediaItem, index: Int): MediaItem = with(mediaItem) {
        val bundle = Bundle().apply {
            putAll(mediaMetadata.extras!!)
            putInt("subtitleIndex", index)
        }
        buildWithBundle(this, bundle)
    }


    fun withRetry(item: MediaItem): MediaItem {
        val bundle = item.mediaMetadata.extras!!
        val retries = bundle.getInt("retries") + 1
        bundle.putBoolean("loaded", false)
        bundle.putInt("retries", retries)
        return buildWithBundle(item, bundle)
    }

    private fun buildWithBundle(mediaItem: MediaItem, bundle: Bundle) = run {
        val item = mediaItem.buildUpon()
        val metadata =
            mediaItem.mediaMetadata.buildUpon().setExtras(bundle).setSubtitle(bundle.indexes())
                .build()
        item.setMediaMetadata(metadata)
        item.build()
    }

    fun String.toIdAndIndex() = if (startsWith('{')) runCatching {
        toData<Triple<String, Int, Int>>()
    }.getOrNull() else null

    fun buildForSource(
        mediaItem: MediaItem, index: Int, source: Streamable.Source
    ) = with(mediaItem) {
        val item = buildUpon()
        item.setUri(
            Triple(mediaId, source.hashCode(), index).toJson()
        )
        when (val decryption = (source as? Streamable.Source.Http)?.decryption) {
            null -> {}
            is Streamable.Decryption.Widevine -> {
                val drmRequest = decryption.license
                val config = MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(drmRequest.url).setMultiSession(decryption.isMultiSession)
                    .setLicenseRequestHeaders(drmRequest.headers).build()
                item.setDrmConfiguration(config)
            }
        }
        item.build()
    }

    fun buildWithBackgroundAndSubtitle(
        mediaItem: MediaItem,
        background: Streamable.Media.Background?,
        subtitle: Streamable.Media.Subtitle?
    ) = with(mediaItem) {
        val bundle = mediaMetadata.extras!!
        bundle.putSerialized("background", background)
        val item = buildUpon()
        item.setSubtitleConfigurations(
            if (subtitle == null) listOf()
            else listOf(
                MediaItem.SubtitleConfiguration.Builder(subtitle.url.toUri())
                    .setMimeType(subtitle.type.toMimeType())
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build()
            )
        )
        item.build()
    }


    @OptIn(UnstableApi::class)
    private fun Track.toMetaData(
        bundle: Bundle,
        extensionId: String = bundle.getString("clientId")!!,
        context: EchoMediaItem? = bundle.getSerialized("context"),
        loaded: Boolean = bundle.getBoolean("loaded"),
        settings: SharedPreferences? = null,
        sourcesIndex: Int? = null,
        backgroundIndex: Int? = null,
        subtitleIndex: Int? = null
    ) = MediaMetadata.Builder()
        .setTitle(title)
        .setAlbumTitle(album?.title)
        .setAlbumArtist(album?.artists?.joinToString(", ") { it.name })
        .setArtist(toMediaItem().subtitleWithE)
        .setArtworkUri(cover?.toUriWithJson())
        .setUserRating(
            if (isLiked) ThumbRating(true) else ThumbRating()
        )
        .setExtras(bundle.apply {
            putSerialized("track", this@toMetaData)
            putString("extensionId", extensionId)
            putSerialized("context", context)
            putBoolean("loaded", loaded)
            putInt("serverIndex", sourcesIndex ?: selectSourceIndex(settings, servers))
            putInt("subtitleIndex", subtitleIndex ?: 0.takeIf { subtitles.isNotEmpty() } ?: -1)
            putInt(
                "backgroundIndex", backgroundIndex ?: 0.takeIf { backgrounds.isNotEmpty() } ?: -1
            )
        })
        .setSubtitle(bundle.indexes())
        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .build()

    private fun Bundle.indexes() =
        "${getInt("serverIndex")} ${getInt("sourceIndex")} ${getInt("backgroundIndex")} ${getInt("subtitleIndex")}"

    val MediaMetadata.isLoaded by delegated { extras?.getBoolean("loaded") ?: false }
    val MediaMetadata.track by delegated { requireNotNull(extras?.getSerialized<Track>("track")) }
    val MediaMetadata.extensionId by delegated { requireNotNull(extras?.getString("extensionId")) }
    val MediaMetadata.context by delegated { extras?.getSerialized<EchoMediaItem?>("context") }
    val MediaMetadata.sourcesIndex by delegated { extras?.getInt("serverIndex") ?: -1 }
    val MediaMetadata.sourceIndex by delegated { extras?.getInt("sourceIndex") ?: -1 }
    val MediaMetadata.backgroundIndex by delegated { extras?.getInt("backgroundIndex") ?: -1 }
    val MediaMetadata.subtitleIndex by delegated { extras?.getInt("subtitleIndex") ?: -1 }
    val MediaMetadata.isLiked by delegated { (userRating as? ThumbRating)?.isThumbsUp == true }
    val MediaMetadata.background by delegated {
        extras?.getSerialized<Streamable.Media.Background?>("background")
    }
    val MediaMetadata.retries by delegated { extras?.getInt("retries") ?: 0 }

    val MediaItem.track get() = mediaMetadata.track
    val MediaItem.extensionId get() = mediaMetadata.extensionId
    val MediaItem.context get() = mediaMetadata.context
    val MediaItem.isLoaded get() = mediaMetadata.isLoaded
    val MediaItem.sourcesIndex get() = mediaMetadata.sourcesIndex
    val MediaItem.sourceIndex get() = mediaMetadata.sourceIndex
    val MediaItem.backgroundIndex get() = mediaMetadata.backgroundIndex
    val MediaItem.subtitleIndex get() = mediaMetadata.subtitleIndex
    val MediaItem.background get() = mediaMetadata.background
    val MediaItem.isLiked get() = mediaMetadata.isLiked
    val MediaItem.retries get() = mediaMetadata.retries

    private fun Streamable.SubtitleType.toMimeType() = when (this) {
        Streamable.SubtitleType.VTT -> MimeTypes.TEXT_VTT
        Streamable.SubtitleType.SRT -> MimeTypes.APPLICATION_SUBRIP
        Streamable.SubtitleType.ASS -> MimeTypes.TEXT_SSA
    }

    private fun ImageHolder.toUriWithJson(): Uri {
        val main = when (this) {
            is ImageHolder.UriImageHolder -> uri
            is ImageHolder.UrlRequestImageHolder -> request.url
        }.toUri()
        val json = toJson()
        return main.buildUpon().appendQueryParameter("actual_data", json).build()
    }
}
