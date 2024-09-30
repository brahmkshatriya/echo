package dev.brahmkshatriya.echo.download

import android.content.Context
import android.content.Intent
import android.os.Environment
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.db.models.DownloadEntity
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.selectAudioStream
import dev.brahmkshatriya.echo.utils.getFromCache
import dev.brahmkshatriya.echo.utils.saveToCache
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.Unit

class Downloader(
    private val extensionList: MutableStateFlow<List<MusicExtension>?>,
    private val throwable: MutableSharedFlow<Throwable>,
    database: EchoDatabase,
    private val context: Context
) {
    val dao = database.downloadDao()
    private val compositeDisposable = CompositeDisposable()
    private var downloading = false
    private val processId = "MyDlProcess"

    init {
        // Pre-initialize YoutubeDL
        try {
            YoutubeDL.getInstance().init(context)
            FFmpeg.getInstance().init(context)
        } catch (e: YoutubeDLException) {
            e.printStackTrace()
        }
    }

    private val callback = object : Function3<Float, Long, String, Unit> {
        override fun invoke(p1: Float, p2: Long, p3: String): Unit {
            // You can update the UI here if needed
            return Unit
        }
    }

    suspend fun addToDownload(
        context: Context, clientId: String, item: EchoMediaItem
    ) = withContext(Dispatchers.IO) {
        val extension = extensionList.getExtension(clientId) ?: return@withContext
        when (item) {
            is EchoMediaItem.Lists -> {
                when (item) {
                    is EchoMediaItem.Lists.AlbumItem -> extension.get<AlbumClient, Unit>(throwable) {
                        val album = loadAlbum(item.album)
                        val tracks = loadTracks(album)
                        tracks.clear()
                        tracks.loadAll().forEach {
                            context.enqueueDownload(extension, it, album.toMediaItem())
                        }
                    }

                    is EchoMediaItem.Lists.PlaylistItem -> extension.get<PlaylistClient, Unit>(throwable) {
                        val playlist = loadPlaylist(item.playlist)
                        val tracks = loadTracks(playlist)
                        tracks.clear()
                        tracks.loadAll().forEach {
                            context.enqueueDownload(extension, it, playlist.toMediaItem())
                        }
                    }

                    is EchoMediaItem.Lists.RadioItem -> Unit
                }
            }

            is EchoMediaItem.TrackItem -> {
                context.enqueueDownload(extension, item.track)
            }

            else -> throw IllegalArgumentException("Not Supported")
        }
    }

    private suspend fun Context.enqueueDownload(
        extension: Extension<*>,
        small: Track,
        parent: EchoMediaItem.Lists? = null
    ) = withContext(Dispatchers.IO) {
        if (downloading) {
            println("Cannot start download. A download is already in progress.")
            return@withContext
        }
        val loaded = extension.get<TrackClient, Track>(throwable) { loadTrack(small) }
            ?: return@withContext
        val album = (parent as? EchoMediaItem.Lists.AlbumItem)?.album ?: loaded.album?.let {
            extension.get<AlbumClient, Album>(throwable) {
                loadAlbum(it)
            }
        } ?: loaded.album
        val track = loaded.copy(album = album)

        val settings = getSharedPreferences(packageName, Context.MODE_PRIVATE)
        val stream = selectAudioStream(settings, track.audioStreamables)
            ?: throw Exception("No Stream Found")
        require(stream.mimeType == Streamable.MimeType.Progressive)
        val media = extension.get<TrackClient, Streamable.Media.AudioOnly>(throwable) {
            getStreamableMedia(stream) as Streamable.Media.AudioOnly
        } ?: return@withContext

        val folder = "Echo${parent?.title?.let { "/$it" } ?: ""}"
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "${folder}/${track.title}")

        val id = when (val audio = media.audio) {
            is Streamable.Audio.Http -> {
                val request = audio.request
                val downloadRequest = YoutubeDLRequest(request.url).apply {
                    addOption("--no-mtime")
                    request.headers.forEach {
                        addOption("--add-header", "${it.key}:${it.value}.")
                    }
                    // Simplified file name to avoid long filename error
                    addOption("-o", file.absolutePath + ".%(ext)s")
                    addOption("-x")
                    //addOption("--audio-format", "mp3")
                    // Options to manage speed and connections
                    addOption("--limit-rate", "1000M") // Adjust as needed
                    addOption("--http-chunk-size", "100M") // Adjust as needed
                }

                showStart()

                downloading = true

                val id = downloadRequest.hashCode().toLong()
                val disposable = Observable.fromCallable {
                    YoutubeDL.getInstance().execute(downloadRequest, processId, callback)
                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        sendDownloadCompleteBroadcast(id)
                        println("Download complete")
                        println(response.out)
                        downloading = false
                    }, { e ->
                        println("Failed to download: ${e.message}")
                        downloading = false
                    })

                compositeDisposable.add(disposable)
                id
            }

            else -> throw Exception("Not Supported")

        }

        dao.insertDownload(DownloadEntity(id, track.id, extension.id, parent?.title, file.absolutePath + ".mp3"))
        saveToCache(track.id, track, "downloads")
    }

    private fun sendDownloadCompleteBroadcast(downloadId: Long) {
        val intent = Intent(context, DownloadReceiver::class.java).apply {
            action = "dev.brahmkshatriya.echo.DOWNLOAD_COMPLETE"
            putExtra("downloadId", downloadId)
        }
        context.sendBroadcast(intent)
    }



    private fun showStart() {
        // Update UI to show download start
        println("Download started")
    }

    suspend fun removeDownload(downloadId: Long) {
        withContext(Dispatchers.IO) {
            dao.deleteDownload(downloadId)
        }
    }

    fun pauseDownload(downloadId: Long) {
        println("pauseDownload: $downloadId")
        // Handle pausing download if supported by YoutubeDL
    }

    suspend fun resumeDownload(context: Context, downloadId: Long) {
        println("resumeDownload: $downloadId")
        val download = dao.getDownload(downloadId) ?: return
        val extension = extensionList.getExtension(download.clientId) ?: return
        val track =
            context.getFromCache<Track>(download.itemId, "downloads") ?: return
        withContext(Dispatchers.IO) {
            context.enqueueDownload(extension, track)
        }
    }
}