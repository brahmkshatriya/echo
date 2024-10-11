package dev.brahmkshatriya.echo.playback.source

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import dev.brahmkshatriya.echo.playback.source.MediaResolver.Companion.LOCAL

@OptIn(UnstableApi::class)
class CustomCacheDataSource(
    cache: SimpleCache,
    private val upstream: DataSource.Factory
) : BaseDataSource(true) {

    class Factory(
        private val cache: SimpleCache,
        private val upstream: DataSource.Factory
    ) : DataSource.Factory {
        override fun createDataSource() = CustomCacheDataSource(cache, upstream)
    }

    private val cacheFactory = CacheDataSource
        .Factory().setCache(cache)
        .setUpstreamDataSourceFactory(upstream)

    var source: DataSource? = null
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return source?.read(buffer, offset, length) ?: throw Exception("Source not opened")
    }

    override fun open(dataSpec: DataSpec): Long {
        val source = if (dataSpec.uri == LOCAL) upstream.createDataSource()
        else cacheFactory.createDataSource()
        this.source = source
        return source.open(dataSpec)
    }

    override fun getUri(): Uri? {
        return source?.uri
    }

    override fun close() {
        source?.close()
        source = null
    }
}