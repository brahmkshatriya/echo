package dev.brahmkshatriya.echo.player

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.utils.getFromCache
import dev.brahmkshatriya.echo.utils.saveToCache
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.trackNotSupported
import kotlinx.coroutines.runBlocking

@UnstableApi
class TrackDataSource(
    private val context: Context,
    private val extensionListFlow: ExtensionModule.ExtensionListFlow,
    private val global: Queue,
    private val dataSource: DataSource,
) : BaseDataSource(true) {

    class Factory(
        private val context: Context,
        private val extensionListFlow: ExtensionModule.ExtensionListFlow,
        private val global: Queue,
        private val factory: DataSource.Factory,
    ) : DataSource.Factory {

        override fun createDataSource() =
            TrackDataSource(context, extensionListFlow, global, factory.createDataSource())
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int) =
        dataSource.read(buffer, offset, length)

    private fun getTrack(id: String) = global.getTrack(id)
        ?: throw Exception(context.getString(R.string.track_not_found))

    private suspend fun getAudio(dataSpec: DataSpec): DataSpec {
        val id = dataSpec.uri.toString()
        val streamableTrack = getTrack(id)
        val loaded = streamableTrack.loaded
        val track = loaded ?: run {
            val client = extensionListFlow.getClient(streamableTrack.clientId)
                ?: throw Exception(context.noClient().message)
            if (client !is TrackClient)
                throw Exception(context.trackNotSupported(client.metadata.name).message)

            val track = getTrackFromCache(id)
                ?: client.loadTrack(streamableTrack.unloaded)
                    .also { context.saveToCache(id, it) }

            current = track
            streamableTrack.loaded = track
            streamableTrack.onLoad.emit(track)
            track
        }
        dataSpec.withUri(track.id.toUri())
        return DataSpec.Builder()
            .setCustomData(streamableTrack)
            .setUri(track.id.toUri())
            .build()
    }

    private var current: Track? = null
    private fun getTrackFromCache(id: String): Track? {
        val track = current?.takeIf { it.id == id } ?:
            context.getFromCache(id) { Track.creator.createFromParcel(it) }
        ?: return null
        return if(!track.isExpired()) track else null
    }

    private fun Track.isExpired() = System.currentTimeMillis() > expiresAt

    override fun open(dataSpec: DataSpec): Long {
        val spec = runBlocking { runCatching { getAudio(dataSpec) } }.getOrThrow()
        return dataSource.open(spec)
    }

    override fun getUri() = dataSource.uri
    override fun close() = dataSource.close()
}
