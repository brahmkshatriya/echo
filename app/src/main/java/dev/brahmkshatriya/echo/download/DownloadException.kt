package dev.brahmkshatriya.echo.download

import dev.brahmkshatriya.echo.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.db.models.TrackDownloadTaskEntity

data class DownloadException(
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