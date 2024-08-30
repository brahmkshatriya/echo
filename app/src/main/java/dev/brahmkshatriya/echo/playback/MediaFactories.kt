package dev.brahmkshatriya.echo.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import dev.brahmkshatriya.echo.common.models.Streamable

@UnstableApi
class MediaFactories(
    dataSource: DataSource.Factory,
    private val callback: (MediaItem) -> Streamable
) {
    fun setDrm(drmSessionManagerProvider: DrmSessionManagerProvider) {
        default.setDrmSessionManagerProvider(drmSessionManagerProvider)
        hls.setDrmSessionManagerProvider(drmSessionManagerProvider)
        dash.setDrmSessionManagerProvider(drmSessionManagerProvider)
    }

    fun setPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy) {
        default.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
        hls.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
        dash.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
    }

    fun create(mediaItem: MediaItem): MediaSource {
        val factory = when (callback(mediaItem).mimeType) {
            Streamable.MimeType.Progressive -> default
            Streamable.MimeType.HLS -> hls
            Streamable.MimeType.DASH -> dash
        }
        return factory.createMediaSource(mediaItem)
    }

    private val default = DefaultMediaSourceFactory(dataSource)
    private val hls = HlsMediaSource.Factory(dataSource)
    private val dash = DashMediaSource.Factory(dataSource)

    val supportedTypes = default.supportedTypes + hls.supportedTypes + dash.supportedTypes
}