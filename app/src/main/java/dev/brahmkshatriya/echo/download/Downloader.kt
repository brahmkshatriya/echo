package dev.brahmkshatriya.echo.download

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.app.NotificationCompat
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
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

    private val activeDownloads = ConcurrentHashMap<Long, Job>()
    private val activeDownloadGroups = mutableMapOf<Long, DownloadGroup>()
    private val notificationBuilders = mutableMapOf<Int, NotificationCompat.Builder>()

    private val illegalChars = "[/\\\\:*?\"<>|]".toRegex()

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
                        val allTracks = tracks.loadAll()
                        val groupId = album.id.id()
                        val groupTitle = album.title

                        val notificationId = (groupId and 0x7FFFFFFF).toInt()
                        val notificationBuilder = DownloadNotificationHelper.buildNotification(
                            context,
                            "Downloading Album: $groupTitle",
                            progress = 0,
                            indeterminate = false
                        )
                        val group = DownloadGroup(
                            id = groupId,
                            title = groupTitle,
                            totalTracks = allTracks.size,
                            downloadedTracks = 0,
                            notificationId = notificationId,
                            notificationBuilder = notificationBuilder
                        )
                        activeDownloadGroups[groupId] = group

                        DownloadNotificationHelper.updateNotification(
                            context,
                            notificationId,
                            notificationBuilder.build()
                        )

                        allTracks.forEach {
                            context.enqueueDownload(extension, it, album.toMediaItem(), group)
                        }
                    }

                    is EchoMediaItem.Lists.PlaylistItem -> extension.get<PlaylistClient, Unit>(
                        throwable
                    ) {
                        val playlist = loadPlaylist(item.playlist)
                        val tracks = loadTracks(playlist)
                        tracks.clear()
                        val allTracks = tracks.loadAll()
                        val groupId = playlist.id.id()
                        val groupTitle = playlist.title
                        val notificationId = (groupId and 0x7FFFFFFF).toInt()
                        val notificationBuilder = DownloadNotificationHelper.buildNotification(
                            context,
                            "Downloading Playlist: $groupTitle",
                            progress = 0,
                            indeterminate = false
                        )
                        val group = DownloadGroup(
                            id = groupId,
                            title = groupTitle,
                            totalTracks = allTracks.size,
                            downloadedTracks = 0,
                            notificationId = notificationId,
                            notificationBuilder = notificationBuilder
                        )
                        activeDownloadGroups[groupId] = group

                        DownloadNotificationHelper.updateNotification(
                            context,
                            notificationId,
                            notificationBuilder.build()
                        )

                        allTracks.forEach {
                            context.enqueueDownload(extension, it, playlist.toMediaItem(), group)
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

    private fun Context.enqueueDownload(
        extension: Extension<*>,
        track: Track,
        parent: EchoMediaItem.Lists? = null,
        group: DownloadGroup? = null
    ) = launch {
        val loadedTrack = extension.get<TrackClient, Track>(throwable) { loadTrack(track) }
            ?: return@launch
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
        } ?: return@launch
        val sanitizedParent = illegalChars.replace(parent?.title.orEmpty(), "_")
        val folder = "Echo${sanitizedParent.let { "/$it" }}"
        var file: File
        val downloadId: Long
        when (val audio = media.audio) {
            is Streamable.Audio.Http -> {
                downloadDirectoryFor(folder)
                val sanitizedTitle = illegalChars.replace(completeTrack.title, "_")
                file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "${folder}/$sanitizedTitle.m4a"
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
                val notificationId: Int
                val notificationBuilder: NotificationCompat.Builder

                if (group != null) {
                    notificationId = group.notificationId
                    notificationBuilder = group.notificationBuilder
                } else {
                    notificationId = (downloadId and 0x7FFFFFFF).toInt()

                    notificationBuilder = DownloadNotificationHelper.buildNotification(
                        applicationContext,
                        "Downloading: ${completeTrack.title}",
                        indeterminate = true
                    )
                    notificationBuilders[notificationId] = notificationBuilder

                    DownloadNotificationHelper.updateNotification(
                        applicationContext,
                        notificationId,
                        notificationBuilder.build()
                    )
                }

                val job = launch {
                    try {
                        val session = FFmpegKit.execute(ffmpegCommand)
                        if (ReturnCode.isSuccess(session.returnCode)) {

                            dao.insertDownload(
                                DownloadEntity(
                                    id = downloadId,
                                    itemId = completeTrack.id,
                                    clientId = extension.id,
                                    groupName = parent?.title,
                                    downloadPath = file.absolutePath.orEmpty()
                                )
                            )

                            applicationContext.saveToCache(
                                completeTrack.id,
                                completeTrack,
                                "downloads"
                            )

                            sendDownloadCompleteBroadcast(downloadId)

                            if (group != null) {
                                group.downloadedTracks += 1

                                withContext(Dispatchers.Main) {
                                    notificationBuilder.setContentText("Downloaded ${group.downloadedTracks}/${group.totalTracks} Songs")
                                        .setProgress(
                                            group.totalTracks,
                                            group.downloadedTracks,
                                            false
                                        )
                                    if (group.downloadedTracks == group.totalTracks) {
                                        notificationBuilder.setContentText("Download complete")
                                            .setProgress(0, 0, false)
                                            .setOngoing(false)
                                        activeDownloadGroups.remove(group.id)
                                    }
                                    DownloadNotificationHelper.updateNotification(
                                        applicationContext,
                                        notificationId,
                                        notificationBuilder.build()
                                    )
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    notificationBuilder.setContentText("Download complete")
                                        .setProgress(0, 0, false)
                                        .setOngoing(false)
                                    DownloadNotificationHelper.updateNotification(
                                        applicationContext,
                                        notificationId,
                                        notificationBuilder.build()
                                    )
                                }
                            }
                        } else if (ReturnCode.isCancel(session.returnCode)) {
                            DownloadNotificationHelper.errorNotification(
                                applicationContext,
                                notificationId,
                                completeTrack.title,
                                "Cancelled"
                            )
                        } else {
                            val failureMessage =
                                session.failStackTrace ?: "Unknown error"
                            println("Failed to download: $failureMessage")
                            throwable.emit(Exception("FFmpeg failed with code ${session.returnCode}"))

                            DownloadNotificationHelper.errorNotification(
                                applicationContext,
                                notificationId,
                                completeTrack.title,
                                failureMessage
                            )
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            throwable.emit(e)
                        }

                        DownloadNotificationHelper.errorNotification(
                            applicationContext,
                            notificationId,
                            completeTrack.title,
                            e.message ?: "Unknown error"
                        )
                    } finally {
                        activeDownloads.remove(downloadId)
                        notificationBuilders.remove(notificationId)
                    }
                }

                activeDownloads[downloadId] = job
            }

            is Streamable.Audio.Channel, is Streamable.Audio.ByteStream -> {
                downloadDirectoryFor(folder)
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

                val sanitizedTitle = illegalChars.replace(completeTrack.title, "_")
                val tempFile =
                    File(downloadsDir, "$folder/$sanitizedTitle.tmp").normalize()

                var totalBytes = 0L
                val inputStream = when (audio) {
                    is Streamable.Audio.Channel -> {
                        totalBytes = audio.totalBytes
                        audio.channel.toInputStream()
                    }

                    is Streamable.Audio.ByteStream -> {
                        totalBytes = audio.totalBytes
                        audio.stream
                    }

                    else -> {
                        null
                    }
                }

                downloadId = audio.toString().id()
                val notificationId: Int
                val notificationBuilder: NotificationCompat.Builder

                if (group != null) {
                    notificationId = group.notificationId
                    notificationBuilder = group.notificationBuilder
                } else {
                    notificationId = (downloadId and 0x7FFFFFFF).toInt()
                    notificationBuilder = DownloadNotificationHelper.buildNotification(
                        applicationContext,
                        "Downloading: ${completeTrack.title}",
                        indeterminate = true
                    )
                    notificationBuilders[notificationId] = notificationBuilder

                    DownloadNotificationHelper.updateNotification(
                        applicationContext,
                        notificationId,
                        notificationBuilder.build()
                    )
                }

                val job = launch {
                    try {
                        BufferedInputStream(inputStream).use { inputStream ->
                            FileOutputStream(tempFile, true).use { outputStream ->

                                val buffer = ByteArray(4096)
                                var received: Long = 0
                                var bytesRead: Int
                                val totalBytesRead = totalBytes

                                var lastProgress = 0
                                var lastUpdateTime = System.currentTimeMillis()

                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    outputStream.write(buffer, 0, bytesRead)
                                    received += bytesRead

                                    if (totalBytesRead > 0 && group == null) {
                                        val progress = ((received * 100) / totalBytesRead).toInt()
                                            .coerceIn(0, 100)
                                        val currentTime = System.currentTimeMillis()

                                        if ((progress >= lastProgress + 10) && (currentTime - lastUpdateTime >= 500L)) {
                                            lastProgress = progress
                                            lastUpdateTime = currentTime


                                            withContext(Dispatchers.Main) {
                                                notificationBuilder.setProgress(
                                                    100,
                                                    progress,
                                                    false
                                                )
                                                    .setContentText("Downloading: $progress%")
                                                DownloadNotificationHelper.updateNotification(
                                                    applicationContext,
                                                    notificationId,
                                                    notificationBuilder.build()
                                                )
                                            }
                                        }
                                    }

                                    if (received >= totalBytesRead) {
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
                                downloadPath = file.absolutePath.orEmpty()
                            )
                        )

                        applicationContext.saveToCache(
                            completeTrack.id,
                            completeTrack,
                            "downloads"
                        )

                        sendDownloadCompleteBroadcast(downloadId)

                        if (group != null) {
                            group.downloadedTracks += 1

                            withContext(Dispatchers.Main) {
                                notificationBuilder.setContentText("Downloaded ${group.downloadedTracks}/${group.totalTracks} Songs")
                                    .setProgress(group.totalTracks, group.downloadedTracks, false)
                                if (group.downloadedTracks == group.totalTracks) {
                                    notificationBuilder.setContentText("Download complete")
                                        .setProgress(0, 0, false)
                                        .setOngoing(false)
                                    activeDownloadGroups.remove(group.id)
                                }
                                DownloadNotificationHelper.updateNotification(
                                    applicationContext,
                                    notificationId,
                                    notificationBuilder.build()
                                )
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                notificationBuilder.setContentText("Download complete")
                                    .setProgress(0, 0, false)
                                    .setOngoing(false)
                                DownloadNotificationHelper.updateNotification(
                                    applicationContext,
                                    notificationId,
                                    notificationBuilder.build()
                                )
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            throwable.emit(e)
                        }

                        if (tempFile.exists()) {
                            tempFile.delete()
                        }

                        DownloadNotificationHelper.errorNotification(
                            applicationContext,
                            notificationId,
                            completeTrack.title,
                            e.message ?: "Unknown error"
                        )
                    } finally {
                        if (tempFile.exists()) {
                            tempFile.delete()
                        }

                        activeDownloads.remove(downloadId)
                        notificationBuilders.remove(notificationId)
                    }
                }

                activeDownloads[downloadId] = job
            }

            else -> throw Exception("Unsupported audio stream type")
        }
    }

    private fun Context.sendDownloadCompleteBroadcast(downloadId: Long) {
        val intent = Intent(this, DownloadReceiver::class.java).apply {
            action = "dev.brahmkshatriya.echo.DOWNLOAD_COMPLETE"
            putExtra("downloadId", downloadId)
        }
        sendBroadcast(intent)
    }

    private suspend fun probeFileFormat(file: File): String? = withContext(Dispatchers.IO) {
        val ffprobeCommand =
            "-v error -show_entries format=format_name -of default=noprint_wrappers=1:nokey=1 \"${file.absolutePath}\""

        val session = FFprobeKit.execute(ffprobeCommand)
        val returnCode = session.returnCode

        if (ReturnCode.isSuccess(returnCode)) {
            val output = session.output?.trim()
            output?.split(",")?.firstOrNull()
        } else {
            null
        }
    }

    private fun downloadDirectoryFor(folder: String?): File {
        val directory =
            File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$folder")
        if (!directory.exists()) directory.mkdirs()
        return directory
    }

    suspend fun removeDownload(context: Context, downloadId: Long) {
        withContext(Dispatchers.IO) {
            activeDownloads[downloadId]?.cancel()
            activeDownloads.remove(downloadId)
            dao.deleteDownload(downloadId)
            withContext(Dispatchers.Main) {
                DownloadNotificationHelper.completeNotification(
                    context,
                    (downloadId and 0x7FFFFFFF).toInt(),
                    "Download Removed"
                )
            }
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

    companion object {
        private data class DownloadGroup(
            val id: Long,
            val title: String,
            val totalTracks: Int,
            var downloadedTracks: Int,
            val notificationId: Int,
            val notificationBuilder: NotificationCompat.Builder
        )
    }
}