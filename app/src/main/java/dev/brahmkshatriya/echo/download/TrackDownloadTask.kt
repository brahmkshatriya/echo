package dev.brahmkshatriya.echo.download

import android.content.Context
import android.media.MediaScannerConnection
import dev.brahmkshatriya.echo.common.MiscExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.db.DownloadDao
import dev.brahmkshatriya.echo.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.db.models.TaskType
import dev.brahmkshatriya.echo.db.models.TrackDownloadTaskEntity
import dev.brahmkshatriya.echo.download.DownloadException.Companion.toDownloadException
import dev.brahmkshatriya.echo.download.task.FiledTask
import dev.brahmkshatriya.echo.download.task.LoadDataTask
import dev.brahmkshatriya.echo.download.task.MediaTask
import dev.brahmkshatriya.echo.utils.rootCause
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.Collections.synchronizedList
import java.util.concurrent.ConcurrentHashMap

class TrackDownloadTask(
    private val context: Context,
    val entity: TrackDownloadTaskEntity,
    private val dao: DownloadDao,
    private val semaphore: Semaphore,
    private val extensionsList: MutableStateFlow<List<MusicExtension>?>,
    private val downloadExtension: MiscExtension,
) {

    private val errors = synchronizedList(mutableListOf<Throwable>())

    private suspend fun download(
        i: Int,
        it: Streamable.Source,
        downloadContext: DownloadContext,
        trackEntity: TrackDownloadTaskEntity,
        folderPath: File,
        downloadExtension: MiscExtension,
    ): Result<File> {
        val id = "${trackEntity.streamableId}-$i".hashCode().toLong()
        val title = it.title ?: "${trackEntity.streamableId.hashCode()}-$i"
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
            loadTask?.takeIf { it.entity.id == id }?.let { task -> task.launch { block(this) } }
            tasks[id]?.let { task -> task.launch { block(this) } }
            mergeTask?.takeIf { it.entity.id == id }?.let { task -> task.launch { block(this) } }
            taggingTask?.takeIf { it.entity.id == id }?.let { task -> task.launch { block(this) } }
        }.mapNotNull { it }

    suspend fun await(): List<Throwable> {
        val success = start()
        return if (success) emptyList()
        else errors.map { it.toDownloadException(entity) }
    }

    private fun ifCancelled(exception: Throwable?): Boolean {
        if (exception != null) {
            if (exception.rootCause is TaskCancelException) return true
            errors.add(exception)
        }
        return false
    }

    suspend fun start(): Boolean = coroutineScope {
        semaphore.withPermit {
            val dContext = entity.contextId?.let { dao.getMediaItemEntity(it) }
            val server = LoadDataTask(dao, entity, dContext, extensionsList, downloadExtension)
                .also { loadTask = it }
                .await().getOrElse {
                    return@coroutineScope ifCancelled(it)
                }

            val trackEntity = dao.getTrackEntity(entity.id) ?: return@coroutineScope false
            val downloadContext =
                trackEntity.run {
                    DownloadContext(
                        extensionId,
                        track,
                        sortOrder,
                        dContext?.mediaItem
                    )
                }
            val folder = File(trackEntity.folderPath!!)
            val toMergeFiles = server.sources.filterIndexed { i, _ -> i in trackEntity.indexes }
                .mapIndexed { i, it ->
                    async {
                        download(
                            i,
                            it,
                            downloadContext,
                            trackEntity,
                            folder,
                            downloadExtension
                        )
                    }
                }.awaitAll()

            val allCancelled = toMergeFiles.map {
                ifCancelled(it.exceptionOrNull())
            }
            if (allCancelled.all { it }) return@coroutineScope true

            val mergeFiles = toMergeFiles.mapNotNull { it.getOrNull() }
            if (mergeFiles.isEmpty()) return@coroutineScope false
            val mergeEntity = MediaTaskEntity(
                "${entity.id}_merge".hashCode().toLong(), entity.id, TaskType.MERGE
            )
            val mergedFile = FiledTask(dao, mergeEntity, downloadExtension) {
                merge(downloadContext, mergeFiles, folder)
            }.also { mergeTask = it }.await().getOrElse {
                return@coroutineScope ifCancelled(it)
            }

            val taggingEntity = MediaTaskEntity(
                "${entity.id}_tag".hashCode().toLong(), entity.id, TaskType.TAGGING
            )
            val file = FiledTask(dao, taggingEntity, downloadExtension) {
                tag(downloadContext, mergedFile)
            }.also { taggingTask = it }.await().getOrElse {
                return@coroutineScope ifCancelled(it)
            }

            MediaScannerConnection.scanFile(
                context, arrayOf(file.toString()), null, null
            )
            true
        }
    }

    suspend fun cancel(taskIds: List<Long>) {
        get(taskIds) { it.cancel() }
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