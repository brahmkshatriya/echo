package dev.brahmkshatriya.echo.download.exceptions

import dev.brahmkshatriya.echo.download.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.download.db.models.TrackDownloadTaskEntity

class DownloadException(
    val trackEntity: TrackDownloadTaskEntity,
    override val cause: Throwable
) : Exception() {
    companion object {
        fun Throwable.toDownloadException(
            trackEntity: TrackDownloadTaskEntity,
            mediaTaskEntity: MediaTaskEntity? = null
        ) = DownloadException(
            trackEntity,
            mediaTaskEntity?.let { TaskException(it, this) } ?: this
        )
    }
}