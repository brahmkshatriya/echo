package dev.brahmkshatriya.echo.ui.download

import androidx.recyclerview.widget.DiffUtil
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.db.models.EchoMediaItemEntity
import dev.brahmkshatriya.echo.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.db.models.Status
import dev.brahmkshatriya.echo.db.models.TrackDownloadTaskEntity
import dev.brahmkshatriya.echo.extensions.getExtension
import kotlinx.coroutines.flow.MutableStateFlow

sealed class DownloadItem {

    abstract val supportsPausing: Boolean
    abstract val isPlaying: Boolean
    abstract val progress: Long
    abstract val total: Long?
    abstract val trackId: Long
    abstract val taskIds: List<Long>

    data class Track(
        val trackEntity: TrackDownloadTaskEntity,
        val extension: Metadata?,
        val context: EchoMediaItem?,
        val tasks: List<MediaTaskEntity>,
        override val supportsPausing: Boolean,
        override val isPlaying: Boolean,
        override val progress: Long,
        override val total: Long?,
        override val taskIds: List<Long>,
        override val trackId: Long = trackEntity.id,
    ) : DownloadItem()

    data class Task(
        val taskEntity: MediaTaskEntity,
        override val isPlaying: Boolean = taskEntity.status != Status.Paused,
        override val supportsPausing: Boolean = taskEntity.supportsPause,
        override val progress: Long = taskEntity.progress,
        override val total: Long? = taskEntity.size,
        override val taskIds: List<Long> = listOf(taskEntity.id),
        override val trackId: Long = taskEntity.trackId,
    ) : DownloadItem()

    companion object {
        fun from(
            contexts: List<EchoMediaItemEntity>,
            tracks: List<TrackDownloadTaskEntity>,
            tasks: List<MediaTaskEntity>,
            extensionList: MutableStateFlow<List<MusicExtension>?>
        ): List<DownloadItem> {
            val map = tracks.associate {
                it.id to (it to mutableListOf<MediaTaskEntity>())
            }
            tasks.forEach { map[it.trackId]?.second?.add(it) }

            return map.values.map { (track, task) ->
                val taskIds = task.map { it.id }
                val ext = extensionList.getExtension(track.extensionId)?.metadata
                val context = contexts.firstOrNull { it.id == track.contextId }?.mediaItem
                val pause =
                    task.takeIf { it.isNotEmpty() }?.any { !it.supportsPause }?.not() ?: false
                val playing =
                    task.takeIf { it.isNotEmpty() }?.any { it.status != Status.Paused } ?: false
                val progress = task.sumOf { it.progress }
                val total = task.mapNotNull { it.size }.sum().takeIf { it != 0L }
                listOf(
                    Track(
                        track, ext, context, task, pause, playing, progress, total, taskIds
                    )
                ) + task.map { Task(it) }
            }.flatten()
        }
    }

    object Diff : DiffUtil.ItemCallback<DownloadItem>() {
        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
            return when (oldItem) {
                is Task -> if (newItem is Task) oldItem.taskEntity.id == newItem.taskEntity.id else false
                is Track -> if (newItem is Track) oldItem.trackEntity.id == newItem.trackEntity.id else false
            }
        }

        override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
            when (oldItem) {
                is Task -> if (newItem is Task) return oldItem == newItem
                is Track -> if (newItem is Track) return oldItem == newItem
            }
            return false
        }
    }
}