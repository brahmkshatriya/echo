package dev.brahmkshatriya.echo.download.workers

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Progress
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.download.db.models.TaskType
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.utils.Serializer.toJson

class LoadingWorker(
    context: Context,
    workerParams: WorkerParameters,
    downloader: Downloader,
) : BaseWorker(context, workerParams, downloader) {

    override val type = TaskType.Loading

    private val workManager = downloader.workManager
    private val extensionsList = downloader.extensions.music

    private val totalSize = 3L
    override suspend fun work(trackId: Long) {
        progressFlow.value = Progress(totalSize, 0)
        var download = dao.getDownloadEntity(trackId)!!
        val extension = extensionsList.getExtensionOrThrow(download.extensionId)
        val streamables = if (!download.loaded) {
            val (track, streamables) = extension.get<TrackClient, Pair<Track, List<Streamable>>> {
                val track = loadTrack(download.track)
                track to track.servers.ifEmpty {
                    throw Exception("${track.title}: No servers found")
                }
            }.getOrThrow()

            download = download.copy(data = track.toJson(), loaded = true)
            dao.insertDownloadEntity(download)
            streamables
        } else download.track.streamables

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
            OneTimeWorkRequestBuilder<DownloadingWorker>()
                .setInputData(DownloadingWorker.createInputData(trackId, index))
                .addTag(trackId.toString())
                .build()
        }

        val mergeRequest = OneTimeWorkRequestBuilder<MergingWorker>()
            .setInputData(createInputData(trackId))
            .addTag(trackId.toString())
            .build()

        val taggingRequest = OneTimeWorkRequestBuilder<TaggingWorker>()
            .setInputData(createInputData(trackId))
            .addTag(trackId.toString())
            .build()

        workManager.beginUniqueWork(trackId.toString(), ExistingWorkPolicy.APPEND, requests)
            .then(mergeRequest)
            .then(taggingRequest)
            .enqueue()

        println("Loading Done")
    }
}