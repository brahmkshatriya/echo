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
import dev.brahmkshatriya.echo.extensions.isClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    database: EchoDatabase,
    private val extensionsList: MutableStateFlow<List<MusicExtension>?>,
    private val miscList: MutableStateFlow<List<MiscExtension>?>,
    private val actions: MutableSharedFlow<TaskAction>,
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val dao = database.downloadDao()
    private val tracksFlow = MutableStateFlow<List<TrackDownloadTaskEntity>>(listOf())

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        extensionsList.first { it != null }
        val downloadExtension = miscList.value?.first { it.isClient<DownloadClient>() }
            ?: return@withContext Result.failure()

        val actionJob = launch { actions.collect { performAction(it) } }

        tracksFlow.value = dao.getTracks()

        val trackJob = launch {
            dao.getTrackFlow().collect {
                tracksFlow.value = it
                it.forEach { entity ->
                    trackTaskMap.getOrPut(entity.id) {
                        TrackDownloadTask(entity, dao, extensionsList, downloadExtension).also {
                            launch { it.initialize() }
                        }
                    }
                }
            }
        }

        tracksFlow.first { it.isEmpty() }

        actionJob.cancel()
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
                is TaskAction.All.PauseAll -> pause(allIds)
                is TaskAction.All.RemoveAll -> cancel(allIds)
                is TaskAction.All.ResumeAll -> resume(allIds)
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