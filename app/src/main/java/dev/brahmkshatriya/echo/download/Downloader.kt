package dev.brahmkshatriya.echo.download

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.download.db.DownloadDatabase
import dev.brahmkshatriya.echo.download.db.models.EchoMediaItemEntity
import dev.brahmkshatriya.echo.download.db.models.TrackDownloadTaskEntity
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.utils.Serializer.toJson
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class Downloader(
    val app: App,
    database: DownloadDatabase,
    extensionLoader: ExtensionLoader
) {
    val scope = CoroutineScope(Dispatchers.IO) + CoroutineName("Downloader")

    val actions = MutableSharedFlow<TaskAction>()
    val extensions = extensionLoader.extensions
    val dao = database.downloadDao()

    val downloadsFlow = dao.getCurrentDownloadsFlow().flowOn(Dispatchers.IO)
        .combine(dao.getTrackFlow()) { tasks, tracks ->
            tasks to tracks
        }.combine(dao.getMediaItemFlow()) { (tasks, tracks), mediaItems ->
            Triple(tasks, tracks, mediaItems)
        }.distinctUntilChanged()

    fun add(
        tracks: List<DownloadContext>
    ) = scope.launch {
        val contexts = tracks.mapNotNull { it.context }.distinctBy { it.id }.associate {
            it.id to dao.insertMediaItemEntity(EchoMediaItemEntity(0, it.toJson()))
        }
        tracks.forEach {
            dao.insertTrackEntity(
                TrackDownloadTaskEntity(
                    0,
                    it.extensionId,
                    contexts[it.context?.id],
                    it.track.toJson(),
                    it.sortOrder
                )
            )
        }
        startWorker()
    }

    fun cancel(downloadIds: List<Long>) = scope.launch {
        startWorker()
        actions.emit(TaskAction.Remove(downloadIds))
    }

    fun retry(trackId: Long) = scope.launch {
        startWorker()
        actions.emit(TaskAction.RetryTrack(trackId))
    }

    fun pause(downloadIds: List<Long>) = scope.launch {
        startWorker()
        actions.emit(TaskAction.Pause(downloadIds))
    }

    fun resume(downloadIds: List<Long>) = scope.launch {
        startWorker()
        actions.emit(TaskAction.Resume(downloadIds))
    }

    fun start() = scope.launch { startWorker() }

    private val workManager = WorkManager.getInstance(app.context)
    private suspend fun startWorker() {
        val tasks = dao.getTracks()
        if (tasks.isEmpty()) return
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(Constraints(NetworkType.CONNECTED, requiresStorageNotLow = true))
            .addTag(TAG)
            .build()
        workManager.enqueueUniqueWork(TAG, androidx.work.ExistingWorkPolicy.KEEP, request)
    }

    fun cancelTrackDownload(trackIds: List<Long>) = scope.launch {
        startWorker()
        trackIds.forEach { id ->
            val track = dao.getTrackEntity(id) ?: return@forEach
            actions.emit(TaskAction.RemoveTrack(track, false))
        }
    }

    companion object {
        const val TAG = "DOWNLOAD_WORKER"
    }
}