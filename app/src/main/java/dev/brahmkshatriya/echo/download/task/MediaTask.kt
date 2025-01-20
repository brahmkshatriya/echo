package dev.brahmkshatriya.echo.download.task

import dev.brahmkshatriya.echo.common.helpers.Progress
import dev.brahmkshatriya.echo.db.DownloadDao
import dev.brahmkshatriya.echo.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.db.models.Status
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

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
        val final = flow.first {
            when (it) {
                is Progress.Initialized ->
                    dao.insertDownload(entity.copy(status = Status.Initialized))

                is Progress.Downloading -> dao.insertDownload(
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
                            is Progress.Final.Failed -> Status.Failed
                        }
                    )
                )
            }
            it is Progress.Final
        } as Progress.Final
        return when (final) {
            is Progress.Final.Failed -> Result.failure(final.reason)
            is Progress.Final.Completed -> Result.success(final.data)
        }
    }
}