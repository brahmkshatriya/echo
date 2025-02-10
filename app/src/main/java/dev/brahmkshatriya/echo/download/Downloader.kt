package dev.brahmkshatriya.echo.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.db.models.EchoMediaItemEntity
import dev.brahmkshatriya.echo.db.models.TrackDownloadTaskEntity
import dev.brahmkshatriya.echo.utils.toJson
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
    context: Context,
    database: EchoDatabase,
    private val actions: MutableSharedFlow<TaskAction>
) {
    val dao = database.downloadDao()
    val downloadsFlow = dao.getCurrentDownloadsFlow().flowOn(Dispatchers.IO)
        .combine(dao.getTrackFlow()) { tasks, tracks ->
            tasks to tracks
        }.combine(dao.getMediaItemFlow()) { (tasks, tracks), mediaItems ->
            Triple(tasks, tracks, mediaItems)
        }.distinctUntilChanged()

    private val scope = CoroutineScope(Dispatchers.IO) + CoroutineName("Downloader")

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
        start()
    }

    fun cancel(downloadIds: List<Long>) = scope.launch {
        start()
        actions.emit(TaskAction.Remove(downloadIds))
    }

    fun pause(downloadIds: List<Long>) = scope.launch {
        start()
        actions.emit(TaskAction.Pause(downloadIds))
    }

    fun resume(downloadIds: List<Long>) = scope.launch {
        start()
        actions.emit(TaskAction.Resume(downloadIds))
    }

    private val workManager = WorkManager.getInstance(context)
    fun start() {
        if (isWorkerWorking()) return
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(Constraints(NetworkType.CONNECTED, requiresStorageNotLow = true))
            .addTag(TAG)
            .build()
        workManager.enqueue(request)
    }

    private fun isWorkerWorking(): Boolean {
        val workInfo = workManager.getWorkInfosByTag(TAG).get()
        workInfo.forEach { work ->
            if (work.state == WorkInfo.State.ENQUEUED || work.state == WorkInfo.State.RUNNING)
                return true
        }
        return false
    }

    fun cancelTrackDownload(trackIds: List<Long>) = scope.launch {
        start()
        trackIds.forEach { id ->
            val track = dao.getTrackEntity(id) ?: return@forEach
            actions.emit(TaskAction.RemoveTrack(track, false))
        }
    }

    companion object {
        const val TAG = "DOWNLOAD_WORKER"
    }
}