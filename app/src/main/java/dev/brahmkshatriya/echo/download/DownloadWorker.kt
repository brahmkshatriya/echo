package dev.brahmkshatriya.echo.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.MiscExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.db.models.TrackDownloadTaskEntity
import dev.brahmkshatriya.echo.download.notifications.NotificationUtil
import dev.brahmkshatriya.echo.extensions.isClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
) : CoroutineWorker(context, workerParams) {

    private val dao = database.downloadDao()
    private val tracksFlow = MutableStateFlow<List<TrackDownloadTaskEntity>>(listOf())

    @OptIn(FlowPreview::class)
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        extensionsList.first { it != null }
        val downloadExtension = miscList.value?.first { it.isClient<DownloadClient>() }
            ?: return@withContext Result.failure()

        val actionJob = launch { actions.collect { performAction(it) } }
        val notificationJob = launch {
            dao.getCurrentDownloadsFlow().debounce(1000L).collect {
                val info = NotificationUtil.create(context, dao, it) ?: return@collect
                setForeground(info)
            }
        }
        tracksFlow.value = dao.getTracks()

        val trackJob = launch {
            dao.getTrackFlow().collect { tasks ->
                tracksFlow.value = tasks
                tasks.forEach { task ->
                    trackTaskMap.getOrPut(task.id) {
                        TrackDownloadTask(task, dao, extensionsList, downloadExtension).apply {
                            launch { await().forEach { throwableFlow.emit(it) } }
                        }
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

    private suspend fun performAction(action: TaskAction) = when (action) {
        is TaskAction.Pause -> pause(action.ids)
        is TaskAction.Remove -> cancel(action.ids)
        is TaskAction.Resume -> resume(action.ids)
        is TaskAction.All -> {
            val allIds = dao.getAllDownloadEntities().map { it.id }
            when (action) {
                TaskAction.All.PauseAll -> pause(allIds)
                TaskAction.All.RemoveAll -> cancel(allIds)
                TaskAction.All.ResumeAll -> resume(allIds)
            }
        }
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