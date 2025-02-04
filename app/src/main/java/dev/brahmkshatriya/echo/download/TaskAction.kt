package dev.brahmkshatriya.echo.download

import dev.brahmkshatriya.echo.db.models.TrackDownloadTaskEntity

sealed class TaskAction {
    data object RemoveAll : TaskAction()
    data class RemoveTrack(val track: TrackDownloadTaskEntity, val success: Boolean) : TaskAction()
    data class Remove(val ids: List<Long>) : TaskAction()
    data class Pause(val ids: List<Long>) : TaskAction()
    data class Resume(val ids: List<Long>) : TaskAction()
}