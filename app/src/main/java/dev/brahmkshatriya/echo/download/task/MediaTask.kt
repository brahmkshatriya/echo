package dev.brahmkshatriya.echo.download.task

import dev.brahmkshatriya.echo.common.helpers.Progress
import dev.brahmkshatriya.echo.db.DownloadDao
import dev.brahmkshatriya.echo.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.db.models.Status
import dev.brahmkshatriya.echo.download.TaskException.Companion.toTaskException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

abstract class MediaTask<T>(
    private val dao: DownloadDao
) {
    abstract val entity: MediaTaskEntity
    abstract suspend fun initialize(): StateFlow<Progress<T>>
    abstract suspend fun start()
    abstract suspend fun cancel()
    abstract suspend fun pause()
    abstract suspend fun resume()

    suspend fun cancelWithDelete() {
        cancel()
        dao.deleteDownload(entity.id)
    }

    suspend fun await(): Result<T> {
        val flow = initialize()
        dao.insertDownload(entity)
        start()
        val final = flow.first {
            println("${entity.id} Progress: $it")
            when (it) {
                is Progress.Initialized ->
                    dao.insertDownload(entity.copy(status = Status.Initialized))

                is Progress.InProgress -> dao.insertDownload(
                    entity.copy(
                        status = Status.Progressing,
                        progress = it.downloaded,
                        speed = it.speed
                    )
                )

                is Progress.Paused -> dao.insertDownload(
                    entity.copy(
                        status = Status.Paused,
                        progress = it.downloaded,
                    )
                )

                is Progress.Final -> dao.insertDownload(
                    entity.copy(
                        status = when (it) {
                            is Progress.Final.Completed -> Status.Completed
                            is Progress.Final.Cancelled -> Status.Cancelled
                            is Progress.Final.Failed -> Status.Failed
                        }
                    )
                )
            }
            it is Progress.Final
        } as Progress.Final
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