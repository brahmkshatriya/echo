package dev.brahmkshatriya.echo.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.download.db.models.TrackDownloadTaskEntity
import dev.brahmkshatriya.echo.download.notifications.NotificationUtil
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.await
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class DownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters,
    downloader: Downloader,
) : CoroutineWorker(context, workerParams) {

    private val throwFlow = downloader.app.throwFlow
    private val messageFlow = downloader.app.messageFlow
    private val scope = downloader.scope
    private val dao = downloader.dao
    private val extensions = downloader.extensions
    private val actions = downloader.actions
    private val tracksFlow = MutableStateFlow<List<TrackDownloadTaskEntity>>(listOf())

    @OptIn(FlowPreview::class)
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        println("DownloadWorker: doWork")

        val downloadExtension =
            extensions.misc.await().find { it.isClient<DownloadClient>() && it.isEnabled }
                ?: return@withContext Result.failure()

        val semaphore = Semaphore(downloadExtension.get<DownloadClient, Int>(throwFlow) {
            concurrentDownloads
        } ?: return@withContext Result.failure())

        val actionJob = scope.launch { actions.collect { performAction(it) } }
        val notificationJob = scope.launch {
            dao.getCurrentDownloadsFlow().debounce(100L).collect {
                val info = NotificationUtil.create(context, dao, it) ?: return@collect
                setForeground(info)
            }
        }
        tracksFlow.value = dao.getTracks()
        val downloadJob = scope.launch {
            dao.getTrackFlow().collect { tasks ->
                tracksFlow.value = tasks
                tasks.forEach { track ->
                    trackTaskMap.getOrPut(track.id) {
                        val task = TrackDownloadTask(
                            context, track, dao, semaphore, extensions.music, downloadExtension
                        )
                        start(task)
                        task
                    }
                }
            }
        }

        tracksFlow.first { it.isEmpty() }
        actionJob.cancel()
        notificationJob.cancel()
        downloadJob.cancel()

        println("DownloadWorker: doWork complete")
        Result.success()
    }

    private suspend fun performAction(action: TaskAction) {
        when (action) {
            is TaskAction.Pause -> pause(action.ids)
            is TaskAction.Remove -> cancel(action.ids)
            is TaskAction.Resume -> resume(action.ids)

            is TaskAction.RetryTrack -> retryTrack(action.id)
            is TaskAction.RemoveTrack -> {
                val entity = action.track
                if (action.success) onDownloadComplete(entity)
                dao.deleteTrackEntity(entity)
            }

            is TaskAction.RemoveAll -> {
                val allIds = dao.getAllDownloadEntities().map { it.id }
                cancel(allIds)
                dao.getTracks().forEach {
                    performAction(TaskAction.RemoveTrack(it, false))
                }
            }
        }
    }

    private suspend fun onDownloadComplete(entity: TrackDownloadTaskEntity) {
        val mediaEntity = entity.contextId?.let { dao.getMediaItemEntity(it) }
        val item = mediaEntity?.let { item ->
            val tracks = dao.getAllTracksForContext(item.id).map { it.id } - entity.id
            if (tracks.isNotEmpty()) return
            dao.deleteMediaItemEntity(item)
            item.mediaItem
        } ?: entity.track.toMediaItem()
        val text = context.getString(R.string.downloaded_x, item.title)
        messageFlow.emit(Message(text))
        NotificationUtil.create(context, text)
    }

    private val trackTaskMap = ConcurrentHashMap<Long, TrackDownloadTask>()

    private suspend fun pause(taskIds: List<Long>) {
        trackTaskMap.values.forEach { it.pause(taskIds) }
    }

    private suspend fun resume(taskIds: List<Long>) {
        trackTaskMap.values.forEach { it.resume(taskIds) }
    }

    private suspend fun cancel(taskIds: List<Long>) {
        trackTaskMap.values.forEach { it.cancel(taskIds) }
    }

    private fun start(task: TrackDownloadTask) {
        scope.launch {
            dao.getAllDownloadsFor(task.entity.id).forEach { dao.deleteDownload(it.id) }
            val errors = task.await()
            if (errors.isNotEmpty()) errors.forEach { throwFlow.emit(it) }
            else {
                val entity = dao.getTrackEntity(task.entity.id)!!
                performAction(TaskAction.RemoveTrack(entity, entity.finalFile != null))
            }
        }
    }

    private fun retryTrack(trackId: Long) {
        val task = trackTaskMap[trackId] ?: return
        start(task)
    }
}