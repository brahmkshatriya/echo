package dev.brahmkshatriya.echo.playback

import android.content.Context
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
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.MediaState
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.selectServerIndex
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import dev.brahmkshatriya.echo.utils.Serializer.toData
import dev.brahmkshatriya.echo.utils.Serializer.toJson

object MediaItemUtils {

    fun build(
        app: App,
        downloads: List<Downloader.Info>,
        state: MediaState.Unloaded<Track>,
        context: EchoMediaItem?,
    ): MediaItem {
        val item = MediaItem.Builder()
        val metadata = state.toMetaData(bundleOf(), downloads, context, false, app)
        item.setMediaMetadata(metadata)
        item.setMediaId(state.item.id)
        item.setUri(state.item.id)
        return item.build()
    }

    fun buildLoaded(
        app: App,
        downloads: List<Downloader.Info>,
        mediaItem: MediaItem,
        state: MediaState.Loaded<Track>
    ): MediaItem = with(mediaItem) {
        val item = buildUpon()
        val metadata = state.toMetaData(
            mediaMetadata.extras!!, downloads, context, true, app
        )
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
        val bundle = Bundle().apply {
            putAll(item.mediaMetadata.extras!!)
            val retries = getInt("retries") + 1
            putBoolean("loaded", false)
            putInt("retries", retries)
        }
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
        toData<Pair<String, Int>>()
    }.getOrNull() else null

    fun buildForSource(
        mediaItem: MediaItem, index: Int, source: Streamable.Source?
    ) = with(mediaItem) {
        val item = buildUpon()
        item.setUri(Pair(mediaId, index).toJson())
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
    private fun MediaState<Track>.toMetaData(
        bundle: Bundle,
        downloads: List<Downloader.Info>,
        context: EchoMediaItem? = bundle.getSerialized("context"),
        loaded: Boolean = bundle.getBoolean("loaded"),
        app: App,
        serverIndex: Int? = null,
        backgroundIndex: Int? = null,
        subtitleIndex: Int? = null
    ) = with(item) {
        val isLiked = (this@toMetaData as? MediaState.Loaded<*>)?.isLiked == true
        MediaMetadata.Builder()
            .setTitle(title)
            .setAlbumTitle(album?.title)
            .setAlbumArtist(album?.artists?.joinToString(", ") { it.name })
            .setArtist(artists.joinToString(", ") { it.name })
            .setArtworkUri(cover?.toUriWithJson())
            .setUserRating(
                if (isLiked) ThumbRating(true) else ThumbRating()
            )
            .setExtras(Bundle().apply {
                putAll(bundle)
                putSerialized("unloadedCover", bundle.stateNullable?.item?.cover)
                putSerialized("state", this@toMetaData)
                putSerialized("context", context)
                putBoolean("loaded", loaded)
                putInt("subtitleIndex", subtitleIndex ?: 0.takeIf { subtitles.isNotEmpty() } ?: -1)
                putInt(
                    "backgroundIndex", backgroundIndex ?: 0.takeIf {
                        backgrounds.isNotEmpty() && app.settings.showBackground()
                    } ?: -1
                )
                val downloaded =
                    downloads.filter { it.download.trackId == id }
                        .mapNotNull { it.download.finalFile }
                putInt(
                    "serverIndex",
                    serverIndex ?: selectServerIndex(app, extensionId, servers, downloaded)
                )
                putSerialized("downloaded", downloaded)
            })
            .setSubtitle(bundle.indexes())
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .build()
    }

    private fun Bundle.indexes() =
        "${getInt("serverIndex")} ${getInt("sourceIndex")} ${getInt("backgroundIndex")} ${getInt("subtitleIndex")}"

    private val Bundle?.stateNullable get() = this?.getSerialized<MediaState<Track>>("state")
    val Bundle?.state get() = requireNotNull(stateNullable)
    val Bundle?.track get() = state.item
    val Bundle?.isLoaded get() = this?.getBoolean("loaded") ?: false
    val Bundle?.extensionId get() = state.extensionId
    val Bundle?.context get() = this?.getSerialized<EchoMediaItem?>("context")
    val Bundle?.serverIndex get() = this?.getInt("serverIndex", -1) ?: -1
    val Bundle?.sourceIndex get() = this?.getInt("sourceIndex", -1) ?: -1
    val Bundle?.backgroundIndex get() = this?.getInt("backgroundIndex", -1) ?: -1
    val Bundle?.subtitleIndex get() = this?.getInt("subtitleIndex", -1) ?: -1
    val Bundle?.background get() = this?.getSerialized<Streamable.Media.Background?>("background")
    val Bundle?.retries get() = this?.getInt("retries") ?: 0
    val Bundle?.unloadedCover get() = this?.getSerialized<ImageHolder?>("unloadedCover")
    val Bundle?.downloaded get() = this?.getSerialized<List<String>>("downloaded")

    val MediaItem.state get() = mediaMetadata.extras.state
    val MediaItem.track get() = mediaMetadata.extras.track
    val MediaItem.extensionId get() = mediaMetadata.extras.extensionId
    val MediaItem.context get() = mediaMetadata.extras.context
    val MediaItem.isLoaded get() = mediaMetadata.extras.isLoaded
    val MediaItem.serverIndex get() = mediaMetadata.extras.serverIndex
    val MediaItem.sourceIndex get() = mediaMetadata.extras.sourceIndex
    val MediaItem.backgroundIndex get() = mediaMetadata.extras.backgroundIndex
    val MediaItem.subtitleIndex get() = mediaMetadata.extras.subtitleIndex
    val MediaItem.background get() = mediaMetadata.extras.background
    val MediaMetadata.isLiked get() = (userRating as? ThumbRating)?.isThumbsUp == true
    val MediaItem.isLiked get() = mediaMetadata.isLiked
    val MediaItem.retries get() = mediaMetadata.extras.retries
    val MediaItem.unloadedCover get() = mediaMetadata.extras.unloadedCover
    val MediaItem.downloaded get() = mediaMetadata.extras.downloaded

    private fun Streamable.SubtitleType.toMimeType() = when (this) {
        Streamable.SubtitleType.VTT -> MimeTypes.TEXT_VTT
        Streamable.SubtitleType.SRT -> MimeTypes.APPLICATION_SUBRIP
        Streamable.SubtitleType.ASS -> MimeTypes.TEXT_SSA
    }

    private fun ImageHolder.toUriWithJson(): Uri {
        val main = when (this) {
            is ImageHolder.ResourceUriImageHolder -> uri
            is ImageHolder.NetworkRequestImageHolder -> request.url
            is ImageHolder.ResourceIdImageHolder -> "res://$resId"
            is ImageHolder.HexColorImageHolder -> ""
        }.toUri()
        val json = toJson()
        return main.buildUpon().appendQueryParameter("actual_data", json).build()
    }

    const val SHOW_BACKGROUND = "show_background"
    fun SharedPreferences?.showBackground() = this?.getBoolean(SHOW_BACKGROUND, true) ?: true

    fun MediaItem.serverWithDownloads(
        context: Context
    ) = track.servers + listOfNotNull(
        Streamable.server(
            "DOWNLOADED", Int.MAX_VALUE, context.getString(R.string.downloads)
        ).takeIf { !downloaded.isNullOrEmpty() }
    )
}
