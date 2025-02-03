package dev.brahmkshatriya.echo.ui.download

import androidx.recyclerview.widget.DiffUtil
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.db.DownloadDao
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
    abstract val taskIds: List<Long>

    data class Track(
        val trackEntity: TrackDownloadTaskEntity,
        val extension: Metadata?,
        val context: EchoMediaItem?,
        override val supportsPausing: Boolean,
        override val isPlaying: Boolean,
        override val progress: Long,
        override val total: Long?,
        override val taskIds: List<Long>,
    ) : DownloadItem()

    data class Task(
        val taskEntity: MediaTaskEntity,
        override val isPlaying: Boolean = taskEntity.status != Status.Paused,
        override val supportsPausing: Boolean = taskEntity.supportsPause,
        override val progress: Long = taskEntity.progress,
        override val total: Long? = taskEntity.size,
        override val taskIds: List<Long> = listOf(taskEntity.id)
    ) : DownloadItem()

    companion object {
        suspend fun fromTasks(
            dao: DownloadDao,
            tasks: List<MediaTaskEntity>,
            extensionList: MutableStateFlow<List<MusicExtension>?>
        ): List<DownloadItem> {
            val contexts = mutableMapOf<Long?, EchoMediaItem?>()
            val tracks =
                mutableMapOf<Long, Pair<TrackDownloadTaskEntity, MutableList<MediaTaskEntity>>>()
            tasks.forEach { task ->
                val trackId = task.trackId
                val track = tracks.getOrPut(trackId) {
                    val trackEntity = dao.getTrackEntity(trackId)
                    trackEntity to mutableListOf()
                }
                track.second.add(task)
            }
            return tracks.values.map { (track, task) ->
                val taskIds = task.map { it.id }
                val extension = extensionList.getExtension(track.extensionId)?.metadata
                val context = contexts.getOrPut(track.contextId) {
                    if (track.contextId == null) null
                    else dao.getMediaItemEntity(track.contextId).mediaItem
                }
                val pause = tasks.any { !it.supportsPause }.not()
                val isPlaying = task.any { it.status != Status.Paused }
                val progress = tasks.sumOf { it.progress }
                val total = tasks.mapNotNull { it.size }.sum().takeIf { it != 0L }
                listOf(
                    Track(
                        track, extension, context, pause, isPlaying, progress, total, taskIds
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