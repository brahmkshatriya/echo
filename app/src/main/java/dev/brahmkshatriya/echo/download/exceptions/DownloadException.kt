package dev.brahmkshatriya.echo.download.exceptions

import dev.brahmkshatriya.echo.download.db.models.DownloadEntity
import dev.brahmkshatriya.echo.download.db.models.TaskType

data class DownloadException(
    val type: TaskType,
    val downloadEntity: DownloadEntity,
    override val cause: Throwable
) : Exception()