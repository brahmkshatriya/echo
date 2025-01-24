package dev.brahmkshatriya.echo.download.task

import dev.brahmkshatriya.echo.common.helpers.Progress
import dev.brahmkshatriya.echo.db.DownloadDao
import dev.brahmkshatriya.echo.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.db.models.Status
import dev.brahmkshatriya.echo.download.TaskException.Companion.toTaskException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass

abstract class MediaTask<T>(
    private val dao: DownloadDao
) {
    abstract val entity: MediaTaskEntity
    abstract suspend fun initialize(): MutableStateFlow<Progress<T>>
    abstract suspend fun start()
    abstract suspend fun cancel()
    abstract suspend fun pause()
    abstract suspend fun resume()

    suspend fun cancelWithDelete() {
        cancel()
        dao.deleteDownload(entity.id)
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    fun <R> launch(block: suspend MediaTask<T>.() -> R): Job {
        return scope.launch { block() }
    }

    @OptIn(FlowPreview::class)
    suspend fun await(): Result<T> {
        val flow = runCatching { initialize() }.getOrElse {
            val entity = runCatching { entity }.getOrNull()
            return Result.failure(entity?.let { it1 -> it.toTaskException(it1) } ?: it)
        }
        dao.insertDownload(entity)
        launch {
            var last: KClass<*> = Progress.Initialized::class
            var size: Long? = null
            flow.debounce {
                val time = if (last == it::class) 0L else 500L
                last = it::class
                time
            }.collect {
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
            is Progress.Final.Completed -> {
                dao.deleteDownload(entity.id)
                Result.success(final.data)
            }

            is Progress.Final.Cancelled -> {
                dao.deleteDownload(entity.id)
                Result.failure(CancellationException().toTaskException(entity))
            }
        }
    }
}