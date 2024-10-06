package dev.brahmkshatriya.echo.download

import android.content.Context
import android.content.Intent
import android.os.Environment
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
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
import dev.brahmkshatriya.echo.offline.MediaStoreUtils.id
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.selectAudioStream
import dev.brahmkshatriya.echo.utils.getFromCache
import dev.brahmkshatriya.echo.utils.saveToCache
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.CoroutineContext

class Downloader(
    private val extensionList: MutableStateFlow<List<MusicExtension>?>,
    private val throwable: MutableSharedFlow<Throwable>,
    database: EchoDatabase,
) : CoroutineScope {
    val dao = database.downloadDao()

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val activeDownloads = mutableMapOf<Long, Job>()

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
        track: Track,
        parent: EchoMediaItem.Lists? = null
    ) {
        coroutineScope {
            val loadedTrack = extension.get<TrackClient, Track>(throwable) { loadTrack(track) }
                ?: return@coroutineScope
            val album =
                (parent as? EchoMediaItem.Lists.AlbumItem)?.album ?: loadedTrack.album?.let {
                    extension.get<AlbumClient, Album>(throwable) { loadAlbum(it) }
                } ?: loadedTrack.album
            val completeTrack = loadedTrack.copy(album = album, cover = track.cover)
            val settings = getSharedPreferences(packageName, Context.MODE_PRIVATE)
            val stream = selectAudioStream(settings, completeTrack.audioStreamables)
                ?: throw Exception("No Stream Found")
            val media = extension.get<TrackClient, Streamable.Media.AudioOnly>(throwable) {
                getStreamableMedia(stream) as Streamable.Media.AudioOnly
            } ?: return@coroutineScope
            val folder = "Echo${parent?.title?.let { "/$it" } ?: ""}"
            var file: File
            val downloadId: Long
            when (val audio = media.audio) {
                is Streamable.Audio.Http -> {
                    file = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "${folder}/${completeTrack.title}.m4a"
                    ).normalize()

                    val request = audio.request

                    val headers = StringBuilder()
                    request.headers.forEach { header ->
                        headers.append("${header.key}: ${header.value}\r\n")
                    }

                    val ffmpegCommand = buildString {
                        if (headers.isNotEmpty()) {
                            append("-headers \"${headers}\" ")
                        }
                        append("-i \"${request.url}\" ")
                        append("-c copy ")
                        append("-bsf:a aac_adtstoasc ")
                        append("\"${file.absolutePath}\"")
                    }

                    downloadId = audio.toString().id()

                    showStart(completeTrack.title)

                    val job = coroutineScope {
                        launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    val session = FFmpegKit.execute(ffmpegCommand)

                                    if (ReturnCode.isSuccess(session.returnCode)) {

                                        dao.insertDownload(
                                            DownloadEntity(
                                                id = downloadId,
                                                itemId = completeTrack.id,
                                                clientId = extension.id,
                                                groupName = parent?.title,
                                                downloadPath = file.absolutePath.orEmpty(),
                                                track = track.toString()
                                            )
                                        )

                                        applicationContext.saveToCache(
                                            completeTrack.id,
                                            completeTrack,
                                            "downloads"
                                        )

                                        sendDownloadCompleteBroadcast(downloadId)
                                        println("Download complete: ${completeTrack.title}")
                                    } else if (ReturnCode.isCancel(session.returnCode)) {
                                        println("Download cancelled: ${completeTrack.title}")
                                    } else {
                                        // Download failed
                                        val failureMessage =
                                            session.failStackTrace ?: "Unknown error"
                                        println("Failed to download: $failureMessage")
                                        throwable.emit(Exception("FFmpeg failed with code ${session.returnCode}"))
                                    }
                                }
                            } catch (e: Exception) {
                                println("Failed to download: ${e.message}")
                                throwable.emit(e)
                            } finally {
                                activeDownloads.remove(downloadId)
                            }
                        }
                    }

                    activeDownloads[downloadId] = job
                }

                is Streamable.Audio.Channel -> {
                    val downloadsDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val tempFile =
                        File(downloadsDir, "$folder/${completeTrack.title}.tmp").normalize()

                    val byteReadChannel = audio.channel

                    downloadId = audio.toString().id()

                    showStart(completeTrack.title)

                    val job = coroutineScope {
                        launch {
                            try {
                                BufferedInputStream(byteReadChannel.toInputStream()).use { inputStream ->
                                    FileOutputStream(tempFile.path, true).use { outputStream ->

                                        val buffer = ByteArray(4096)
                                        var received: Long = 0
                                        var bytesRead: Int
                                        val totalBytesRead = audio.totalBytes

                                        while (inputStream.read(buffer)
                                                .also { bytesRead = it } != -1
                                        ) {
                                            outputStream.write(buffer, 0, bytesRead)
                                            received += bytesRead
                                            if (received == totalBytesRead) {
                                                break
                                            }
                                        }
                                    }
                                }

                                val detectedExtension = probeFileFormat(tempFile) ?: "mp3"
                                val finalFile = File(
                                    tempFile.parent,
                                    "${tempFile.nameWithoutExtension}.$detectedExtension"
                                )

                                tempFile.renameTo(finalFile)

                                file = finalFile

                                dao.insertDownload(
                                    DownloadEntity(
                                        id = downloadId,
                                        itemId = completeTrack.id,
                                        clientId = extension.id,
                                        groupName = parent?.title,
                                        downloadPath = file.absolutePath.orEmpty(),
                                        track = track.toString()
                                    )
                                )

                                applicationContext.saveToCache(
                                    completeTrack.id,
                                    completeTrack,
                                    "downloads"
                                )

                                sendDownloadCompleteBroadcast(downloadId)
                                println("Download complete: ${completeTrack.title}")
                            } catch (e: Exception) {
                                println("Failed to download Channel variant: ${e.message}")
                                throwable.emit(e)
                            } finally {
                                activeDownloads.remove(downloadId)
                            }
                        }
                    }

                    activeDownloads[downloadId] = job
                }

                is Streamable.Audio.ByteStream -> {
                    val downloadsDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val tempFile =
                        File(downloadsDir, "$folder/${completeTrack.title}.tmp").normalize()

                    val inputStream = audio.stream

                    downloadId = audio.toString().id()

                    showStart(completeTrack.title)

                    val job = coroutineScope {
                        launch {
                            try {
                                BufferedInputStream(inputStream).use { inputStream ->
                                    FileOutputStream(tempFile.path, true).use { outputStream ->

                                        val buffer = ByteArray(4096)
                                        var received: Long = 0
                                        var bytesRead: Int
                                        val totalBytesRead = audio.totalBytes

                                        while (inputStream.read(buffer)
                                                .also { bytesRead = it } != -1
                                        ) {
                                            outputStream.write(buffer, 0, bytesRead)
                                            received += bytesRead
                                            if (received == totalBytesRead) {
                                                break
                                            }
                                        }
                                    }
                                }

                                val detectedExtension = probeFileFormat(tempFile) ?: "mp3"
                                val finalFile = File(
                                    tempFile.parent,
                                    "${tempFile.nameWithoutExtension}.$detectedExtension"
                                )

                                tempFile.renameTo(finalFile)

                                file = finalFile

                                dao.insertDownload(
                                    DownloadEntity(
                                        id = downloadId,
                                        itemId = completeTrack.id,
                                        clientId = extension.id,
                                        groupName = parent?.title,
                                        downloadPath = file.absolutePath.orEmpty(),
                                        track = track.toString()
                                    )
                                )

                                applicationContext.saveToCache(
                                    completeTrack.id,
                                    completeTrack,
                                    "downloads"
                                )

                                sendDownloadCompleteBroadcast(downloadId)
                                println("Download complete: ${completeTrack.title}")
                            } catch (e: Exception) {
                                println("Failed to download Channel variant: ${e.message}")
                                throwable.emit(e)
                            } finally {
                                activeDownloads.remove(downloadId)
                            }
                        }
                    }

                    activeDownloads[downloadId] = job
                }

                else -> throw Exception("Unsupported audio stream type")
            }
        }
    }

    private fun Context.sendDownloadCompleteBroadcast(downloadId: Long) {
        val intent = Intent(this, DownloadReceiver::class.java).apply {
            action = "dev.brahmkshatriya.echo.DOWNLOAD_COMPLETE"
            putExtra("downloadId", downloadId)
        }
        sendBroadcast(intent)
    }

    private fun showStart(title: String) {
        println("Download started: $title")
    }

    suspend fun removeDownload(context: Context, downloadId: Long) {
        withContext(Dispatchers.IO) {
            activeDownloads[downloadId]?.cancel()
            activeDownloads.remove(downloadId)
            dao.deleteDownload(downloadId)
        }
    }

    fun pauseDownload(context: Context, downloadId: Long) {
        println("pauseDownload: $downloadId")
    }

    suspend fun resumeDownload(context: Context, downloadId: Long) {
        val download = dao.getDownload(downloadId) ?: return
        val extension = extensionList.getExtension(download.clientId) ?: return
        val track =
            context.getFromCache<Track>(download.itemId, "downloads") ?: return
        withContext(Dispatchers.IO) {
            context.enqueueDownload(extension, track)
        }
    }
}

suspend fun probeFileFormat(file: File): String? = withContext(Dispatchers.IO) {
    val ffprobeCommand = "-v error -show_entries format=format_name -of default=noprint_wrappers=1:nokey=1 \"${file.absolutePath}\""

    val session = FFprobeKit.execute(ffprobeCommand)
    val returnCode = session.returnCode

    if (ReturnCode.isSuccess(returnCode)) {
        val output = session.output?.trim()
        output?.split(",")?.firstOrNull()
    } else {
        null
    }
}