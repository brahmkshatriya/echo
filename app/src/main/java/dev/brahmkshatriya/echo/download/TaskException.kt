package dev.brahmkshatriya.echo.download

import dev.brahmkshatriya.echo.db.models.MediaTaskEntity

data class TaskException(
    val taskEntity: MediaTaskEntity,
    override val cause: Throwable
) : Exception() {
    companion object {
        fun Throwable.toTaskException(taskEntity: MediaTaskEntity) =
            TaskException(taskEntity, this)
    }
}