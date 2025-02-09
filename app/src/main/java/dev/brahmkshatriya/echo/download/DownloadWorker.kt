package dev.brahmkshatriya.echo.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.MiscExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.db.models.TrackDownloadTaskEntity
import dev.brahmkshatriya.echo.download.notifications.NotificationUtil
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.extensions.isClient
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    database: EchoDatabase,
    private val extensionsList: MutableStateFlow<List<MusicExtension>?>,
    private val miscList: MutableStateFlow<List<MiscExtension>?>,
    private val actions: MutableSharedFlow<TaskAction>,
    private val throwableFlow: MutableSharedFlow<Throwable>,
    private val messageFlow: MutableSharedFlow<Message>,
) : CoroutineWorker(context, workerParams) {

    private val dao = database.downloadDao()
    private val tracksFlow = MutableStateFlow<List<TrackDownloadTaskEntity>>(listOf())
    private val scope = CoroutineScope(Dispatchers.IO) + CoroutineName("DownloadWorker")

    @OptIn(FlowPreview::class)
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        extensionsList.first { it != null }
        miscList.first { it != null }
        val downloadExtension = miscList.value?.find { it.isClient<DownloadClient>() }
            ?: return@withContext Result.failure()

        val semaphore = Semaphore(downloadExtension.get<DownloadClient, Int>(throwableFlow) {
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
        val trackJob = scope.launch {
            dao.getTrackFlow().collect { tasks ->
                tracksFlow.value = tasks
                tasks.forEach { track ->
                    trackTaskMap.getOrPut(track.id) {
                        val task = TrackDownloadTask(
                            context, track, dao, semaphore, extensionsList, downloadExtension
                        )
                        launch {
                            val errors = task.await()
                            if (errors.isNotEmpty()) errors.forEach { throwableFlow.emit(it) }
                            else performAction(TaskAction.RemoveTrack(track, true))
                        }
                        task
                    }
                }
            }
        }

        tracksFlow.first { it.isEmpty() }

        actionJob.cancel()
        notificationJob.cancel()
        trackJob.cancel()
        Result.success()
    }

    private suspend fun performAction(action: TaskAction) {
        when (action) {
            is TaskAction.Pause -> pause(action.ids)
            is TaskAction.Remove -> cancel(action.ids)
            is TaskAction.Resume -> resume(action.ids)
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
}