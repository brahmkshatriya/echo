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
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class Downloader(
    context: Context,
    database: EchoDatabase,
    private val actions: MutableSharedFlow<TaskAction>
) {
    val dao = database.downloadDao()
    val downloadsFlow = dao.getCurrentDownloadsFlow()

    private val scope = CoroutineScope(Dispatchers.IO) + CoroutineName("Downloader")

    fun add(
        tracks: List<DownloadContext>
    ) = scope.launch {
        tracks.forEach {
            val id = it.context?.let { cont ->
                dao.insertMediaItemEntity(EchoMediaItemEntity(0, cont.toJson()))
            }
            dao.insertTrackEntity(
                TrackDownloadTaskEntity(0, it.extensionId, id, it.track.toJson(), it.sortOrder)
            )
        }
        start()
    }

    fun cancel(downloadIds: List<Long>) = scope.launch {
        println("Cancel: ${isWorkerWorking()}")
        actions.emit(TaskAction.Remove(downloadIds))
    }

    fun pause(downloadIds: List<Long>) = scope.launch {
        println("Pause: ${isWorkerWorking()}")
        actions.emit(TaskAction.Pause(downloadIds))
    }

    fun resume(downloadIds: List<Long>) = scope.launch {
        println("Resume: ${isWorkerWorking()}")
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

    companion object {
        const val TAG = "DOWNLOAD_WORKER"
    }
}