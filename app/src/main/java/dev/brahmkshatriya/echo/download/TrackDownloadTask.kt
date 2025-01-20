package dev.brahmkshatriya.echo.download

import dev.brahmkshatriya.echo.common.MiscExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.db.DownloadDao
import dev.brahmkshatriya.echo.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.db.models.TaskType
import dev.brahmkshatriya.echo.db.models.TrackDownloadTaskEntity
import dev.brahmkshatriya.echo.download.task.FiledTask
import dev.brahmkshatriya.echo.download.task.LoadDataTask
import dev.brahmkshatriya.echo.download.task.MediaTask
import io.ktor.util.collections.ConcurrentSet
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class TrackDownloadTask(
    val entity: TrackDownloadTaskEntity,
    private val dao: DownloadDao,
    private val extensionsList: MutableStateFlow<List<MusicExtension>?>,
    private val downloadExtension: MiscExtension,
) {

    private val errors = ConcurrentSet<Throwable>()

    private suspend fun download(
        i: Int,
        it: Streamable.Source,
        downloadContext: DownloadContext,
        trackEntity: TrackDownloadTaskEntity,
        folderPath: File,
        downloadExtension: MiscExtension,
    ): Result<File> {
        val id = "${trackEntity.streamableId}-$i".hashCode().toLong()
        val title = it.title ?: "${trackEntity.streamableId}-$i"
        val taskEntity = MediaTaskEntity(id, entity.id, TaskType.DOWNLOAD, title)
        val file = File(folderPath, title)
        val task = FiledTask(dao, taskEntity, downloadExtension) {
            download(downloadContext, it, file)
        }
        tasks[id] = task
        return task.await()
    }

    private suspend fun <T : Any> get(ids: List<Long>, block: suspend (MediaTask<*>) -> T) =
        ids.map { id ->
            loadTask?.takeIf { it.entity.id == id }?.let { task -> block(task) }
            tasks[id]?.let { task -> block(task) }
            mergeTask?.takeIf { it.entity.id == id }?.let { task -> block(task) }
            taggingTask?.takeIf { it.entity.id == id }?.let { task -> block(task) }
        }.mapNotNull { it }

    suspend fun initialize(): Throwable? {
        val success = start()
        if (success) {
            dao.deleteTrackEntity(entity)
            entity.contextId?.let {
                val shouldDeleteContext = dao.getAllTracksForContext(it).isEmpty()
                if (!shouldDeleteContext) return@let
                val mediaEntity = dao.getMediaItemEntity(it)
                dao.deleteMediaItemEntity(mediaEntity)
            }
        }
        return errors.firstOrNull()
    }

    suspend fun start(): Boolean = coroutineScope {
        val context = dao.getMediaItemEntity(entity.id)
        val server = LoadDataTask(dao, entity, context, extensionsList, downloadExtension)
            .also { loadTask = it }
            .await().getOrElse {
                errors.add(it)
                return@coroutineScope false
            }
        val trackEntity = dao.getTrackEntity(entity.id)
        val downloadContext =
            trackEntity.run { DownloadContext(clientId, track, context.mediaItem) }
        val folderPath = File(trackEntity.folderPath!!)
        val toMergeFiles = server.sources.filterIndexed { i, _ -> i in trackEntity.indexes }
            .mapIndexed { i, it ->
                async {
                    download(
                        i, it, downloadContext, trackEntity, folderPath, downloadExtension
                    ).getOrElse {
                        errors.add(it)
                        null
                    }
                }
            }.awaitAll().mapNotNull { it }
        if (toMergeFiles.isEmpty()) return@coroutineScope false
        val mergeEntity = MediaTaskEntity(
            "${entity.id}_merge".hashCode().toLong(), entity.id, TaskType.MERGE
        )
        val mergedFile = FiledTask(dao, mergeEntity, downloadExtension) {
            merge(downloadContext, toMergeFiles, folderPath)
        }.also { mergeTask = it }.await().getOrElse {
            errors.add(it)
            return@coroutineScope false
        }
        val taggingEntity = MediaTaskEntity(
            "${entity.id}_tag".hashCode().toLong(), entity.id, TaskType.TAGGING
        )
        FiledTask(dao, taggingEntity, downloadExtension) {
            tag(downloadContext, mergedFile)
        }.also { taggingTask = it }.await().getOrElse {
            errors.add(it)
        }
        return@coroutineScope true
    }

    suspend fun cancel(taskIds: List<Long>) {
        get(taskIds) { it.cancelWithDelete() }
    }

    suspend fun resume(taskIds: List<Long>) {
        get(taskIds) { it.resume() }
    }

    suspend fun pause(taskIds: List<Long>) {
        get(taskIds) { it.pause() }
    }

    private var loadTask: LoadDataTask? = null
    private val tasks = ConcurrentHashMap<Long, FiledTask>()
    private var mergeTask: FiledTask? = null
    private var taggingTask: FiledTask? = null
}