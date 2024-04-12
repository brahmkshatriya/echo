package dev.brahmkshatriya.echo.player

import android.content.Context
import android.content.SharedPreferences
import android.os.Parcel
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.STREAM_QUALITY
import dev.brahmkshatriya.echo.utils.tryWith
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.trackNotSupported
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@UnstableApi
class TrackDataSource(
    private val context: Context,
    private val extensionListFlow: ExtensionModule.ExtensionListFlow,
    private val global: Queue,
    private val settings: SharedPreferences,
    private val dataSource: DataSource,
) : BaseDataSource(true) {

    class Factory(
        private val context: Context,
        private val extensionListFlow: ExtensionModule.ExtensionListFlow,
        private val global: Queue,
        private val prefs: SharedPreferences,
        private val factory: DataSource.Factory,
    ) : DataSource.Factory {
        override fun createDataSource() =
            TrackDataSource(context, extensionListFlow, global, prefs, factory.createDataSource())
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int) =
        dataSource.read(buffer, offset, length)

    private fun getTrack(id: String) = global.getTrack(id)
        ?: throw Exception(context.getString(R.string.track_not_found))

    private suspend fun getAudio(id: String) {
        val streamableTrack = getTrack(id)
        if (streamableTrack.loaded != null) return

        val client = extensionListFlow.getClient(streamableTrack.clientId)
            ?: throw Exception(context.noClient().message)
        if (client !is TrackClient)
            throw Exception(context.trackNotSupported(client.metadata.name).message)

        val track = getTrackFromCache(id)
            ?: client.loadTrack(streamableTrack.unloaded)
                .also { saveTrackToCache(id, it) }

        current = track
        streamableTrack.loaded = track
        streamableTrack.streamable = selectStream(track.streamables)
        streamableTrack.onLoad.emit(track)
    }

    private fun selectStream(streamables: List<Streamable>) =
        when (settings.getString(STREAM_QUALITY, "highest")) {
            "highest" -> streamables.maxByOrNull { it.quality }
            "medium" -> streamables.sortedBy { it.quality }.getOrNull(streamables.size / 2)
            "lowest" -> streamables.minByOrNull { it.quality }
            else -> streamables.firstOrNull()
        }

    private var current: Track? = null
    private fun getTrackFromCache(id: String): Track? {
        current?.let { if (it.id == id) return it }
        val fileName = id.hashCode().toString()
        val file = File(context.cacheDir, fileName)
        return if (file.exists()) {
            tryWith {
                val bytes = FileInputStream(file).use { it.readBytes() }
                val parcel = Parcel.obtain()
                parcel.unmarshall(bytes, 0, bytes.size)
                parcel.setDataPosition(0)
                val track = Track.creator.createFromParcel(parcel)
                parcel.recycle()
                track
            }
        } else {
            null
        }
    }

    private fun saveTrackToCache(id: String, track: Track) {
        if (!track.allowCaching) return
        val fileName = id.hashCode().toString()
        val parcel = Parcel.obtain()
        track.writeToParcel(parcel, 0)
        val bytes = parcel.marshall()
        parcel.recycle()
        FileOutputStream(File(context.cacheDir, fileName)).use { it.write(bytes) }
    }

    override fun open(dataSpec: DataSpec): Long {
        runBlocking { runCatching { getAudio(dataSpec.uri.toString()) } }.getOrThrow()
        return dataSource.open(dataSpec)
    }

    override fun getUri() = dataSource.uri
    override fun close() = dataSource.close()
}
