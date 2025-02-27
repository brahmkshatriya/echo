package dev.brahmkshatriya.echo.download.task

import dev.brahmkshatriya.echo.common.helpers.Progress
import dev.brahmkshatriya.echo.download.db.DownloadDao
import dev.brahmkshatriya.echo.download.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.download.db.models.Status
import dev.brahmkshatriya.echo.download.exceptions.TaskCancelException
import dev.brahmkshatriya.echo.download.exceptions.TaskException.Companion.toTaskException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

abstract class MediaTask<T>(
    private val dao: DownloadDao,
    private val downloadScope: CoroutineScope
) {
    abstract val entity: MediaTaskEntity
    abstract suspend fun initialize(): MutableSharedFlow<Progress<T>>
    abstract suspend fun start()
    abstract suspend fun cancel()
    abstract suspend fun pause()
    abstract suspend fun resume()

    suspend fun ensureCancel() {
        if (scope.isActive) {
            cancel()
            scope.cancel()
        }
        dao.deleteDownload(entity.id)
    }

    private val scope = CoroutineScope(Dispatchers.IO) + CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
        downloadScope.launch { flow?.emit(Progress.Final.Failed(throwable)) }
    }

    fun <R> launch(block: suspend MediaTask<T>.() -> R): Job {
        return scope.launch { block() }
    }

    private var flow: MutableSharedFlow<Progress<T>>? = null

    suspend fun await(): Result<T> {
        val flow = scope.async {
            runCatching { initialize() }
        }.await().getOrElse {
            val entity = runCatching { entity }.getOrNull()
            return Result.failure(entity?.let { it1 -> it.toTaskException(it1) } ?: it)
        }
        this.flow = flow
        dao.insertDownload(entity)
        var size: Long? = null
        launch { start() }

        val final = flow.first {
            val final = when (it) {
                is Progress.Initialized -> {
                    size = it.size
                    dao.insertDownload(entity.copy(status = Status.Initialized, size = size))
                    null
                }

                is Progress.InProgress -> {
                    dao.insertDownload(
                        entity.copy(
                            status = Status.Progressing,
                            progress = it.downloaded,
                            speed = it.speed,
                            size = size
                        )
                    )
                    null
                }

                is Progress.Paused -> {
                    dao.insertDownload(
                        entity.copy(
                            status = Status.Paused,
                            progress = it.downloaded,
                            speed = null,
                            size = size
                        )
                    )
                    null
                }

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
                    scope.cancel()
                    it
                }
            }
            final != null
        } as Progress.Final<T>

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