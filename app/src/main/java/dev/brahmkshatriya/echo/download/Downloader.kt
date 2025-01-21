package dev.brahmkshatriya.echo.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track
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
    private val dao = database.downloadDao()
    val downloadsFlow = dao.getCurrentDownloadsFlow()

    private val scope = CoroutineScope(Dispatchers.IO) + CoroutineName("Downloader")

    fun add(
        clientId: String,
        context: EchoMediaItem?,
        tracks: List<Track>
    ) = scope.launch {
        val id = context?.let {
            dao.insertMediaItemEntity(EchoMediaItemEntity(0, context.toJson()))
        }
        tracks.forEach { track ->
            dao.insertTrackEntity(TrackDownloadTaskEntity(0, clientId, id, track.toJson()))
        }
        start()
    }

    fun pauseAll() = scope.launch {
        actions.emit(TaskAction.All.PauseAll)
    }

    fun resumeAll() = scope.launch {
        actions.emit(TaskAction.All.ResumeAll)
    }

    fun cancelAll() = scope.launch {
        actions.emit(TaskAction.All.RemoveAll)
    }

    fun cancel(downloadIds: List<Long>) = scope.launch {
        actions.emit(TaskAction.Remove(downloadIds))
    }

    fun pause(downloadIds: List<Long>) = scope.launch {
        actions.emit(TaskAction.Pause(downloadIds))
    }

    fun resume(downloadIds: List<Long>) = scope.launch {
        actions.emit(TaskAction.Resume(downloadIds))
    }

    private val workManager = WorkManager.getInstance(context)
    fun start() {
        val workInfo = workManager.getWorkInfosByTag(TAG).get()
        workInfo.forEach { work ->
            if (work.state == WorkInfo.State.ENQUEUED || work.state == WorkInfo.State.RUNNING)
                return
        }
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(Constraints(NetworkType.CONNECTED, requiresStorageNotLow = true))
            .addTag(TAG)
            .build()
        workManager.enqueue(request)
    }

    companion object {
        const val TAG = "DOWNLOAD_WORKER"
    }
}