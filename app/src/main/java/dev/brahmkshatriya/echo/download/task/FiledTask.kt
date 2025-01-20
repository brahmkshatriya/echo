package dev.brahmkshatriya.echo.download.task

import dev.brahmkshatriya.echo.common.MiscExtension
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.common.helpers.FileProgress
import dev.brahmkshatriya.echo.common.helpers.FileTask
import dev.brahmkshatriya.echo.common.helpers.Progress
import dev.brahmkshatriya.echo.db.DownloadDao
import dev.brahmkshatriya.echo.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.extensions.get
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class FiledTask(
    dao: DownloadDao,
    private val taskEntity: MediaTaskEntity,
    private val downloadExtension: MiscExtension,
    private val getter: suspend DownloadClient.() -> FileTask,
) : MediaTask<File>(dao) {

    private suspend fun <T> withDownloadExtension(block: suspend DownloadClient.() -> T) =
        downloadExtension.get<DownloadClient, T> { block() }

    private lateinit var task: FileTask
    override val entity: MediaTaskEntity
        get() = taskEntity.copy(supportsPause = task.supportsPause(),)

    override suspend fun initialize(): StateFlow<FileProgress> {
        val task = withDownloadExtension { getter() }.getOrElse {
            return MutableStateFlow(Progress.Final.Failed(it))
        }
        this.task = task
        return task.progressFlow
    }

    private var job: Job? = null
    override suspend fun start() {
        job = coroutineScope { launch { task.start() } }
    }

    override suspend fun cancel() {
        task.cancel()
        job?.cancel()
    }

    override suspend fun pause() {
        task.pause?.invoke()
        job?.cancel()
    }

    override suspend fun resume() {
        job?.cancel()
        job = coroutineScope { launch { task.resume?.invoke() } }
    }
}