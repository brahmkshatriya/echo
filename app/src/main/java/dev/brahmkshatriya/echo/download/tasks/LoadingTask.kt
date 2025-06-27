package dev.brahmkshatriya.echo.download.tasks

import android.content.Context
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Progress
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.download.db.models.TaskType
import dev.brahmkshatriya.echo.download.tasks.TaskManager.Companion.toQueueItem
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.utils.Serializer.toJson

class LoadingTask(
    private val context: Context,
    downloader: Downloader,
    override val trackId: Long,
) : BaseTask(context, downloader, trackId) {

    override val type = TaskType.Loading

    private val manager = downloader.taskManager
    private val extensionsList = downloader.extensionLoader.music

    private val totalSize = 3L

    override suspend fun work(trackId: Long) {
        progressFlow.value = Progress(totalSize, 0)
        var download = dao.getDownloadEntity(trackId)!!
        val extension = extensionsList.getExtensionOrThrow(download.extensionId)
        if (!download.loaded) {
            val track = extension.get<TrackClient, Track> { loadTrack(download.track) }.getOrThrow()
            track.servers.ifEmpty { throw Exception("${track.title}: No servers found") }
            download = download.copy(data = track.toJson(), loaded = true)
            dao.insertDownloadEntity(download)
        }

        progressFlow.value = Progress(totalSize, 1)
        val downloadContext = getDownloadContext()
        if (download.streamableId == null) {
            val selected = withDownloadExtension { selectServer(downloadContext) }
            download = download.copy(streamableId = selected.id)
            dao.insertDownloadEntity(download)
        }
        progressFlow.value = Progress(totalSize, 2)

        val server = downloader.getServer(trackId, download)

        val indexes = download.indexes.ifEmpty {
            val sources = withDownloadExtension { selectSources(downloadContext, server) }
            sources.map { server.sources.indexOf(it) }
        }
        if (indexes.isEmpty()) throw Exception("No files to download")
        download = download.copy(indexesData = indexes.toJson())
        dao.insertDownloadEntity(download)

        progressFlow.value = Progress(totalSize, 3)

        val requests = indexes.map { index ->
            DownloadingTask(context, downloader, trackId, index)
        }.toQueueItem()
        val mergeRequest = MergingTask(context, downloader, trackId).toQueueItem()
        val taggingRequest = TaggingTask(context, downloader, trackId).toQueueItem()
        val saveToUnified = SaveToUnifiedTask(context, downloader, trackId).toQueueItem()

        manager.enqueue(trackId, listOf(requests, mergeRequest, taggingRequest, saveToUnified))
    }
}