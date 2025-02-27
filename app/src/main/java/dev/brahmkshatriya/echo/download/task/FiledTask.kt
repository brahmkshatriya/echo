package dev.brahmkshatriya.echo.download.task

import dev.brahmkshatriya.echo.common.MiscExtension
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.common.helpers.FileProgress
import dev.brahmkshatriya.echo.common.helpers.FileTask
import dev.brahmkshatriya.echo.common.helpers.Progress
import dev.brahmkshatriya.echo.download.db.DownloadDao
import dev.brahmkshatriya.echo.download.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.File

class FiledTask(
    dao: DownloadDao,
    scope: CoroutineScope,
    private val taskEntity: MediaTaskEntity,
    private val downloadExtension: MiscExtension,
    private val getter: suspend DownloadClient.() -> FileTask,
) : MediaTask<File>(dao, scope) {

    private suspend fun <T> withDownloadExtension(block: suspend DownloadClient.() -> T) =
        downloadExtension.get<DownloadClient, T> { block() }

    private lateinit var task: FileTask
    override val entity: MediaTaskEntity
        get() = taskEntity.copy(supportsPause = task.supportsPause())

    override suspend fun initialize(): MutableSharedFlow<FileProgress> {
        val task = withDownloadExtension { getter() }.getOrThrow()
        this.task = task
        return task.progressFlow
    }

    private suspend fun withFailure(block: suspend FileTask.() -> Unit) {
        runCatching { block(task) }.onFailure {
            task.progressFlow.emit(Progress.Final.Failed(it))
        }
    }

    private var job: Job? = null
    override suspend fun start() {
        job = coroutineScope { launch { withFailure { start() } } }
    }

    override suspend fun cancel() {
        runCatching { task.cancel() }.onFailure {
            task.progressFlow.emit(Progress.Final.Cancelled())
        }
        job?.cancel()
    }

    override suspend fun pause() {
        withFailure { pause?.invoke() }
        job?.cancel()
    }

    override suspend fun resume() {
        job?.cancel()
        job = coroutineScope { launch { withFailure { resume?.invoke() } } }
    }
}