package dev.brahmkshatriya.echo.download.task

import dev.brahmkshatriya.echo.common.helpers.Progress
import dev.brahmkshatriya.echo.db.DownloadDao
import dev.brahmkshatriya.echo.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.db.models.Status
import dev.brahmkshatriya.echo.download.TaskCancelException
import dev.brahmkshatriya.echo.download.TaskException.Companion.toTaskException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

abstract class MediaTask<T>(
    private val dao: DownloadDao
) {
    abstract val entity: MediaTaskEntity
    abstract suspend fun initialize(): MutableSharedFlow<Progress<T>>
    abstract suspend fun start()
    abstract suspend fun cancel()
    abstract suspend fun pause()
    abstract suspend fun resume()

    private val scope = CoroutineScope(Dispatchers.IO)
    fun <R> launch(block: suspend MediaTask<T>.() -> R): Job {
        return scope.launch { block() }
    }

    private var flow: MutableSharedFlow<Progress<T>>? = null

    suspend fun await(semaphore: Semaphore): Result<T> = semaphore.withPermit {
        val flow = runCatching { initialize() }.getOrElse {
            val entity = runCatching { entity }.getOrNull()
            return Result.failure(entity?.let { it1 -> it.toTaskException(it1) } ?: it)
        }
        this.flow = flow
        dao.insertDownload(entity)
        launch {
            var size: Long? = null
            flow.collect {
                when (it) {
                    is Progress.Initialized -> {
                        size = it.size
                        dao.insertDownload(entity.copy(status = Status.Initialized, size = size))
                    }

                    is Progress.InProgress -> dao.insertDownload(
                        entity.copy(
                            status = Status.Progressing,
                            progress = it.downloaded,
                            speed = it.speed,
                            size = size
                        )
                    )

                    is Progress.Paused -> dao.insertDownload(
                        entity.copy(
                            status = Status.Paused,
                            progress = it.downloaded,
                            speed = null,
                            size = size
                        )
                    )

                    is Progress.Final -> {
                        dao.insertDownload(
                            entity.copy(
                                status = when (it) {
                                    is Progress.Final.Completed -> Status.Completed
                                    is Progress.Final.Cancelled -> Status.Cancelled
                                    is Progress.Final.Failed -> Status.Failed
                                }
                            )
                        )
                    }
                }
            }
        }
        launch { start() }

        val final = flow.first { it is Progress.Final } as Progress.Final<T>
        scope.cancel()

        return when (final) {
            is Progress.Final.Failed -> Result.failure(final.reason.toTaskException(entity))
            is Progress.Final.Cancelled -> {
                dao.deleteDownload(entity.id)
                Result.failure(TaskCancelException().toTaskException(entity))
            }

            is Progress.Final.Completed -> {
                dao.deleteDownload(entity.id)
                Result.success(final.data)
            }
        }
    }
}