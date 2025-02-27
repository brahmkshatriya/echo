package dev.brahmkshatriya.echo.download.exceptions

import dev.brahmkshatriya.echo.download.db.models.MediaTaskEntity

class TaskException(
    val taskEntity: MediaTaskEntity,
    override val cause: Throwable
) : Exception() {
    companion object {
        fun Throwable.toTaskException(taskEntity: MediaTaskEntity) =
            TaskException(taskEntity, this)
    }
}