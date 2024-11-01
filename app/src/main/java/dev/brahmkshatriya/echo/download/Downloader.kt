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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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

    private val downloadSemaphore = Semaphore(2)

    suspend fun addToDownload(
        context: Context, clientId: String, item: EchoMediaItem
    ) {
        val extension = extensionList.getExtension(clientId) ?: return
        when (item) {
            is EchoMediaItem.Lists -> handleListItem(context, extension, item)
            is EchoMediaItem.TrackItem -> enqueueDownload(context, extension, item.track, 0)
            else -> throw IllegalArgumentException("Not Supported")
        }
    }

    private suspend fun handleListItem(
        context: Context,
        extension: Extension<*>,
        item: EchoMediaItem.Lists
    ) {
        when (item) {
            is EchoMediaItem.Lists.AlbumItem -> handleAlbumDownload(context, extension, item)
            is EchoMediaItem.Lists.PlaylistItem -> handlePlaylistDownload(context, extension, item)
            is EchoMediaItem.Lists.RadioItem -> Unit
        }
    }

    private suspend fun handleAlbumDownload(
        context: Context,
        extension: Extension<*>,
        item: EchoMediaItem.Lists.AlbumItem
    ) {
        extension.get<AlbumClient, Unit>(throwable) {
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

            allTracks.forEachIndexed { index, track ->
                enqueueDownload(context, extension, track, order = index + 1, parent = item, group = group)
            }
        }
    }

    private suspend fun handlePlaylistDownload(
        context: Context,
        extension: Extension<*>,
        item: EchoMediaItem.Lists.PlaylistItem
    ) {
        extension.get<PlaylistClient, Unit>(throwable) {
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

            allTracks.forEachIndexed { index, track ->
                enqueueDownload(context, extension, track, order = index + 1, parent = item, group = group)
            }
        }
    }

    private fun enqueueDownload(
        context: Context,
        extension: Extension<*>,
        track: Track,
        order: Int,
        parent: EchoMediaItem.Lists? = null,
        group: DownloadGroup? = null
    ) {
        launch {
            try {
                val loadedTrack =
                    extension.get<TrackClient, Track>(throwable) { loadTrack(track) }
                        ?: return@launch

                val album =
                    (parent as? EchoMediaItem.Lists.AlbumItem)?.album
                        ?: loadedTrack.album?.let {
                            extension.get<AlbumClient, Album>(throwable) { loadAlbum(it) }
                        } ?: loadedTrack.album

                val completeTrack = loadedTrack.copy(album = album, cover = track.cover)
                val settings = context.getSharedPreferences(
                    context.packageName,
                    Context.MODE_PRIVATE
                )
                val stream = selectAudioStream(settings, completeTrack.audioStreamables)
                    ?: throw Exception("No audio stream available for download")

                val media = extension.get<TrackClient, Streamable.Media.AudioOnly>(throwable) {
                    getStreamableMedia(stream) as Streamable.Media.AudioOnly
                } ?: return@launch

                val sanitizedParent = illegalChars.replace(parent?.title.orEmpty(), "_")
                val folder =
                    if (sanitizedParent.isNotBlank()) "Echo/$sanitizedParent" else "Echo"
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDirectory = File(downloadsDir, folder).apply { mkdirs() }

                val sanitizedTitle = illegalChars.replace(completeTrack.title, "_")
                val fileExtension = when (media.audio) {
                    is Streamable.Audio.Http -> "m4a"
                    is Streamable.Audio.Channel, is Streamable.Audio.ByteStream -> "mp3"
                    else -> "m4a"
                }

                val uniqueFile = getUniqueFile(targetDirectory, sanitizedTitle, fileExtension)
                val downloadId = completeTrack.id.id()
                val notificationId: Int
                val notificationBuilder: NotificationCompat.Builder

                if (group != null) {
                    notificationId = group.notificationId
                    notificationBuilder = group.notificationBuilder
                } else {
                    notificationId = (downloadId and 0x7FFFFFFF).toInt()
                    notificationBuilder = DownloadNotificationHelper.buildNotification(
                        context,
                        "Downloading: ${completeTrack.title}",
                        indeterminate = true
                    )
                    notificationBuilders[notificationId] = notificationBuilder

                    DownloadNotificationHelper.updateNotification(
                        context,
                        notificationId,
                        notificationBuilder.build()
                    )
                }

                val job = when (val audio = media.audio) {
                    is Streamable.Audio.Http -> handleHttpDownload(
                        context,
                        extension,
                        audio,
                        completeTrack,
                        uniqueFile,
                        downloadId,
                        notificationId,
                        group,
                        order
                    )

                    is Streamable.Audio.Channel, is Streamable.Audio.ByteStream -> handleStreamDownload(
                        context,
                        extension,
                        audio,
                        completeTrack,
                        targetDirectory,
                        sanitizedTitle,
                        fileExtension,
                        downloadId,
                        notificationId,
                        notificationBuilder,
                        group,
                        order
                    )

                    else -> throw Exception("Unsupported audio stream type")
                }

                activeDownloads[downloadId] = job
            } catch (e: Exception) {
                throwable.emit(e)
                val notificationId = (track.id.id() and 0x7FFFFFFF).toInt()
                DownloadNotificationHelper.errorNotification(
                    context,
                    notificationId,
                    track.title,
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    private fun handleHttpDownload(
        context: Context,
        extension: Extension<*>,
        audio: Streamable.Audio.Http,
        completeTrack: Track,
        file: File,
        downloadId: Long,
        notificationId: Int,
        group: DownloadGroup?,
        order: Int
    ): Job = launch {
        downloadSemaphore.withPermit {
            val headers =
                audio.request.headers.entries.joinToString("\r\n") { "${it.key}: ${it.value}" }
            val ffmpegCommand = buildString {
                if (headers.isNotEmpty()) append("-headers \"$headers\" ")
                append("-i \"${audio.request.url}\" ")
                append("-c copy ")
                append("-bsf:a aac_adtstoasc ")
                append("\"${file.absolutePath}\"")
            }

            try {
                val session = FFmpegKit.execute(ffmpegCommand)
                if (ReturnCode.isSuccess(session.returnCode)) {
                    dao.insertDownload(
                        DownloadEntity(
                            id = downloadId,
                            itemId = completeTrack.id,
                            clientId = extensionList.value?.find { it.id == extension.id }?.id
                                ?: "",
                            groupName = group?.title,
                            downloadPath = file.absolutePath
                        )
                    )

                    context.saveToCache(completeTrack.id, completeTrack, "downloads")
                    sendDownloadCompleteBroadcast(context, downloadId, order)

                    updateGroupProgress(context, group, notificationId)
                } else if (ReturnCode.isCancel(session.returnCode)) {
                    DownloadNotificationHelper.errorNotification(
                        context,
                        notificationId,
                        completeTrack.title,
                        "Download cancelled"
                    )
                } else {
                    val failureMessage = session.failStackTrace ?: "Unknown error"
                    throwable.emit(Exception("FFmpeg failed with code ${session.returnCode}"))
                    DownloadNotificationHelper.errorNotification(
                        context,
                        notificationId,
                        completeTrack.title,
                        failureMessage
                    )
                }
            } catch (e: Exception) {
                throwable.emit(e)
                DownloadNotificationHelper.errorNotification(
                    context,
                    notificationId,
                    completeTrack.title,
                    e.message ?: "Unknown error"
                )
            } finally {
                activeDownloads.remove(downloadId)
                notificationBuilders.remove(notificationId)
            }
        }
    }

    private fun handleStreamDownload(
        context: Context,
        extension: Extension<*>,
        audio: Streamable.Audio,
        completeTrack: Track,
        targetDirectory: File,
        sanitizedTitle: String,
        fileExtension: String,
        downloadId: Long,
        notificationId: Int,
        notificationBuilder: NotificationCompat.Builder,
        group: DownloadGroup?,
        order: Int
    ): Job = launch {
        downloadSemaphore.withPermit {
            val tempFile = File(targetDirectory, "$sanitizedTitle.tmp").apply { createNewFile() }
            val inputStream = when (audio) {
                is Streamable.Audio.Channel -> audio.channel.toInputStream()
                is Streamable.Audio.ByteStream -> audio.stream
                else -> null
            } ?: throw IllegalArgumentException("Unsupported audio stream type")

            var received: Long = 0
            val totalBytes = when (audio) {
                is Streamable.Audio.Channel -> audio.totalBytes
                is Streamable.Audio.ByteStream -> audio.totalBytes
                else -> -1L
            }

            try {
                BufferedInputStream(inputStream).use { bis ->
                    FileOutputStream(tempFile, false).use { fos ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytesRead: Int
                        val progressUpdateInterval = 500L
                        var lastUpdateTime = System.currentTimeMillis()
                        var lastProgress = 0

                        while (bis.read(buffer).also { bytesRead = it } != -1) {
                            fos.write(buffer, 0, bytesRead)
                            received += bytesRead

                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastUpdateTime >= progressUpdateInterval) {
                                val progress = if (totalBytes > 0) {
                                    ((received * 100) / totalBytes).toInt().coerceIn(0, 100)
                                } else {
                                    -1
                                }

                                if (group == null && (progress >= lastProgress + 10 || progress == 100)) {
                                    lastProgress = progress
                                    lastUpdateTime = currentTime

                                    withContext(Dispatchers.Main) {
                                        if (progress >= 0) {
                                            notificationBuilder.setProgress(100, progress, false)
                                                .setContentText("Downloading: $progress%")
                                        } else {
                                            notificationBuilder.setProgress(0, 0, true)
                                                .setContentText("Downloading...")
                                        }
                                        DownloadNotificationHelper.updateNotification(
                                            context,
                                            notificationId,
                                            notificationBuilder.build()
                                        )
                                    }
                                } else if (group != null) {
                                    lastUpdateTime = currentTime
                                }
                            }

                            if (totalBytes in 1..received) {
                                break
                            }
                        }
                    }
                }

                val detectedExtension = probeFileFormat(tempFile) ?: fileExtension
                val finalFile = getUniqueFile(targetDirectory, sanitizedTitle, detectedExtension)
                if (tempFile.renameTo(finalFile)) {
                    dao.insertDownload(
                        DownloadEntity(
                            id = downloadId,
                            itemId = completeTrack.id,
                            clientId = extensionList.value?.find { it.id == extension.id }?.id
                                ?: "",
                            groupName = group?.title,
                            downloadPath = finalFile.absolutePath
                        )
                    )

                    context.saveToCache(completeTrack.id, completeTrack, "downloads")
                    sendDownloadCompleteBroadcast(context, downloadId, order)

                    updateGroupProgress(context, group, notificationId)

                    if (group == null) {
                        withContext(Dispatchers.Main) {
                            notificationBuilder.setContentText("Download complete")
                                .setProgress(0, 0, false)
                                .setOngoing(false)
                            DownloadNotificationHelper.updateNotification(
                                context,
                                notificationId,
                                notificationBuilder.build()
                            )
                        }
                    }
                } else {
                    throw Exception("Failed to rename temporary file")
                }
            } catch (e: Exception) {
                throwable.emit(e)
                if (tempFile.exists()) tempFile.delete()
                DownloadNotificationHelper.errorNotification(
                    context,
                    notificationId,
                    completeTrack.title,
                    e.message ?: "Unknown error"
                )
            } finally {
                activeDownloads.remove(downloadId)
                notificationBuilders.remove(notificationId)
            }
        }
    }

    private fun sendDownloadCompleteBroadcast(context: Context, downloadId: Long, order: Int) {
        Intent(context, DownloadReceiver::class.java).also { intent ->
            intent.action = "dev.brahmkshatriya.echo.DOWNLOAD_COMPLETE"
            intent.putExtra("downloadId", downloadId)
            intent.putExtra("order", order)
            context.sendBroadcast(intent)
        }
    }

    private suspend fun updateGroupProgress(context: Context, group: DownloadGroup?, notificationId: Int) {
        group?.let {
            it.downloadedTracks += 1
            if (it.downloadedTracks >= it.totalTracks) {
                withContext(Dispatchers.Main) {
                    it.notificationBuilder.setContentText("Download complete")
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                    DownloadNotificationHelper.updateNotification(
                        context,
                        notificationId,
                        it.notificationBuilder.build()
                    )
                }
                activeDownloadGroups.remove(it.id)
            } else {
                withContext(Dispatchers.Main) {
                    it.notificationBuilder.setContentText("Downloaded ${it.downloadedTracks}/${it.totalTracks} Songs")
                        .setProgress(it.totalTracks, it.downloadedTracks, false)
                    DownloadNotificationHelper.updateNotification(
                        context,
                        notificationId,
                        it.notificationBuilder.build()
                    )
                }
            }
        }
    }

    private fun probeFileFormat(file: File): String?  {
        val ffprobeCommand =
            "-v error -show_entries format=format_name -of default=noprint_wrappers=1:nokey=1 \"${file.absolutePath}\""

        val session = FFprobeKit.execute(ffprobeCommand)
        return if (ReturnCode.isSuccess(session.returnCode)) {
            session.output?.trim()?.split(",")?.firstOrNull()
        } else {
            null
        }
    }

    private fun getUniqueFile(directory: File, baseName: String, extension: String): File {
        var uniqueName = "$baseName.$extension"
        var file = File(directory, uniqueName)
        var counter = 1

        while (file.exists()) {
            uniqueName = "$baseName ($counter).$extension"
            file = File(directory, uniqueName)
            counter++
        }

        return file
    }

    suspend fun removeDownload(context: Context, downloadId: Long) {
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

    fun pauseDownload(context: Context, downloadId: Long) {
        activeDownloads[downloadId]?.cancel()
        println("Paused download: $downloadId")
    }


    fun resumeDownload(context: Context, downloadId: Long) {
        val download = dao.getDownload(downloadId) ?: return
        val extension = extensionList.getExtension(download.clientId) ?: return
        val track = context.getFromCache<Track>(download.itemId, "downloads") ?: return
        enqueueDownload(context, extension, track, 0)
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