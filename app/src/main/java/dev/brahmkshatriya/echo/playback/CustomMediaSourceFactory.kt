package dev.brahmkshatriya.echo.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

@OptIn(UnstableApi::class)
class CustomMediaSourceFactory(val context: Context) : MediaSource.Factory {
    private val audioSource = DefaultMediaSourceFactory(context)
    private val videoSource = DefaultMediaSourceFactory(context)

    override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory {
        audioSource.setDrmSessionManagerProvider(drmSessionManagerProvider)
        videoSource.setDrmSessionManagerProvider(drmSessionManagerProvider)
        return this
    }

    override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory {
        audioSource.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
        videoSource.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
        return this
    }

    fun setSourceFactory(a: DataSource.Factory, v: DataSource.Factory): CustomMediaSourceFactory {
        audioSource.setDataSourceFactory(a)
        videoSource.setDataSourceFactory(v)
        return this
    }

    override fun getSupportedTypes() = videoSource.supportedTypes

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        return MergingMediaSource(
            audioSource.createMediaSource(mediaItem),
            videoSource.createMediaSource(mediaItem)
        )
    }
}