package dev.brahmkshatriya.echo.download

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.StreamableAudio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.db.models.DownloadEntity
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.selectStream
import dev.brahmkshatriya.echo.utils.getFromCache
import dev.brahmkshatriya.echo.utils.saveToCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class Downloader(
    private val extensionList: MutableStateFlow<List<MusicExtension>?>,
    database: EchoDatabase,
) {
    val dao = database.downloadDao()

    suspend fun addToDownload(
        context: Context, clientId: String, item: EchoMediaItem
    ) = withContext(Dispatchers.IO) {
        val client = extensionList.getExtension(clientId)?.client
        client as TrackClient
        when (item) {
            is EchoMediaItem.Lists -> {
                when (item) {
                    is EchoMediaItem.Lists.AlbumItem -> {
                        client as AlbumClient
                        val album = client.loadAlbum(item.album)
                        val tracks = client.loadTracks(album)
                        tracks.clear()
                        tracks.loadAll().forEach {
                            context.enqueueDownload(clientId, client, it, album.toMediaItem())
                        }
                    }

                    is EchoMediaItem.Lists.PlaylistItem -> {
                        client as PlaylistClient
                        val playlist = client.loadPlaylist(item.playlist)
                        val tracks = client.loadTracks(playlist)
                        tracks.clear()
                        tracks.loadAll().forEach {
                            context.enqueueDownload(clientId, client, it, playlist.toMediaItem())
                        }
                    }
                }
            }

            is EchoMediaItem.TrackItem -> {
                context.enqueueDownload(clientId, client, item.track)
            }

            else -> throw IllegalArgumentException("Not Supported")
        }
    }

    private suspend fun Context.enqueueDownload(
        clientId: String,
        client: TrackClient,
        small: Track,
        parent: EchoMediaItem.Lists? = null
    ) = withContext(Dispatchers.IO) {
        val loaded = client.loadTrack(small)
        val album = (parent as? EchoMediaItem.Lists.AlbumItem)?.album ?: loaded.album?.let {
            (client as? AlbumClient)?.loadAlbum(it)
        } ?: loaded.album
        val track = loaded.copy(album = album)

        val settings = getSharedPreferences(packageName, Context.MODE_PRIVATE)
        val stream = selectStream(settings, track.audioStreamables)
            ?: throw Exception("No Stream Found")
        val audio = client.getStreamableAudio(stream)
        val folder = "Echo${parent?.title?.let { "/$it" } ?: ""}"

        val id = when (audio) {
            is StreamableAudio.ByteStreamAudio -> {
                TODO("inputStream to file")
            }

            is StreamableAudio.StreamableRequest -> {
                val request = audio.request
                val downloadRequest = DownloadManager.Request(request.url.toUri()).apply {
                    request.headers.forEach {
                        addRequestHeader(it.key, it.value)
                    }
                    setTitle(track.title)
                    setDescription(track.artists.joinToString(", ") { it.name })
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    downloadDirectoryFor(folder)
                    setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "$folder/${track.title}"
                    )
                }
                downloadManager().enqueue(downloadRequest)
            }
        }

        dao.insertDownload(DownloadEntity(id, track.id, clientId, parent?.title))
        saveToCache(track.id, track, "downloads")
    }

    private fun downloadDirectoryFor(folder: String?): File {
        val directory = File("${Environment.DIRECTORY_DOWNLOADS}/$folder")
        if (!directory.exists()) directory.mkdirs()
        return directory
    }

    private fun Context.downloadManager() =
        getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    suspend fun removeDownload(context: Context, downloadId: Long) {
        context.downloadManager().remove(downloadId)
        withContext(Dispatchers.IO) {
            dao.deleteDownload(downloadId)
        }
    }

    fun pauseDownload(context: Context, downloadId: Long) {
        println("pauseDownload: $downloadId")
        context.downloadManager().remove(downloadId)
    }

    suspend fun resumeDownload(context: Context, downloadId: Long) = withContext(Dispatchers.IO) {
        println("resumeDownload: $downloadId")
        val download = dao.getDownload(downloadId) ?: return@withContext
        val client = extensionList.getExtension(download.clientId)?.client
        client as TrackClient
        val track =
            context.getFromCache(download.itemId, Track.creator, "downloads")
                ?: return@withContext
        context.enqueueDownload(download.clientId, client, track)
    }
}