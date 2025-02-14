package dev.brahmkshatriya.echo.download

import android.content.Context
import android.media.MediaScannerConnection
import dev.brahmkshatriya.echo.common.MiscExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.db.DownloadDao
import dev.brahmkshatriya.echo.db.models.EchoMediaItemEntity
import dev.brahmkshatriya.echo.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.db.models.TaskType
import dev.brahmkshatriya.echo.db.models.TrackDownloadTaskEntity
import dev.brahmkshatriya.echo.download.DownloadException.Companion.toDownloadException
import dev.brahmkshatriya.echo.download.task.FiledTask
import dev.brahmkshatriya.echo.download.task.LoadDataTask
import dev.brahmkshatriya.echo.download.task.MediaTask
import dev.brahmkshatriya.echo.utils.toData
import dev.brahmkshatriya.echo.utils.toJson
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

    private var loadTask: LoadDataTask? = null
    private val tasks = ConcurrentHashMap<Long, FiledTask>()
    private var mergeTask: FiledTask? = null
    private var taggingTask: FiledTask? = null
    private suspend fun <T : Any> get(ids: List<Long>, block: suspend (MediaTask<*>) -> T) =
        ids.map { id ->
            loadTask?.takeIf { it.entity.id == id }?.let { task -> task.launch { block(this) } }
            tasks[id]?.let { task -> task.launch { block(this) } }
            mergeTask?.takeIf { it.entity.id == id }?.let { task -> task.launch { block(this) } }
            taggingTask?.takeIf { it.entity.id == id }?.let { task -> task.launch { block(this) } }
        }.mapNotNull { it }

    suspend fun await(): List<Throwable> {
        start()
        return errors.map { it.toDownloadException(entity) }
    }

    suspend fun start() = semaphore.withPermit { getFinalFile() }

    suspend fun cancel(taskIds: List<Long>) {
        get(taskIds) { it.ensureCancel() }
    }

    suspend fun resume(taskIds: List<Long>) {
        get(taskIds) { it.resume() }
    }

    suspend fun pause(taskIds: List<Long>) {
        get(taskIds) { it.pause() }
    }

    private suspend fun getFinalFile() {
        val entity = dao.getTrackEntity(entity.id) ?: return
        if (entity.finalFile != null) return

        val dContext = this.entity.contextId?.let { dao.getMediaItemEntity(it) }
        val downloadContext = entity.run {
            DownloadContext(extensionId, track, sortOrder, dContext?.mediaItem)
        }
        getTagFile(dContext, downloadContext)

        val trackEntity = dao.getTrackEntity(this.entity.id) ?: return
        val tagFile = trackEntity.toTagFile?.let { File(it) } ?: return


        val taggingEntity = MediaTaskEntity(
            "${this.entity.id}_tag".hashCode().toLong(), this.entity.id, TaskType.TAGGING
        )
        val file = FiledTask(dao, taggingEntity, downloadExtension) {
            tag(downloadContext, tagFile)
        }.also { taggingTask = it }.await().getOrElse {
            errors.add(it)
            return
        }

        MediaScannerConnection.scanFile(
            context, arrayOf(file.toString()), null, null
        )

        dao.insertTrackEntity(
            trackEntity.copy(finalFile = file.absolutePath)
        )
    }

    private suspend fun getTagFile(
        context: EchoMediaItemEntity?,
        downloadContext: DownloadContext
    ) {
        getMergeFiles(context, downloadContext)
        val trackEntity = dao.getTrackEntity(entity.id) ?: return

        val toMergeFiles = trackEntity.toMergeFiles?.toData<List<String>>()
            ?.map { File(it) } ?: return

        val folder = File(trackEntity.folderPath!!)

        val mergeEntity = MediaTaskEntity(
            "${entity.id}_merge".hashCode().toLong(), entity.id, TaskType.MERGE
        )
        val mergedFile = FiledTask(dao, mergeEntity, downloadExtension) {
            merge(downloadContext, toMergeFiles, folder)
        }.also { mergeTask = it }.await().getOrElse {
            errors.add(it)
            return
        }

        dao.insertTrackEntity(
            trackEntity.copy(toTagFile = mergedFile.absolutePath)
        )
    }

    private suspend fun getMergeFiles(
        context: EchoMediaItemEntity?,
        downloadContext: DownloadContext
    ) = coroutineScope {
        val entity = dao.getTrackEntity(entity.id) ?: return@coroutineScope
        if (!entity.toMergeFiles?.toData<List<String>>().isNullOrEmpty()) return@coroutineScope
        val server = LoadDataTask(dao, entity, context, extensionsList, downloadExtension)
            .also { loadTask = it }
            .await().getOrElse {
                errors.add(it)
                return@coroutineScope
            }

        val trackEntity = dao.getTrackEntity(entity.id) ?: return@coroutineScope
        val folder = File(trackEntity.folderPath!!)
        val mergeFiles =
            server.sources.filterIndexed { i, _ -> i in trackEntity.indexes }.mapIndexed { i, it ->
                async {
                    download(
                        i, it, downloadContext, trackEntity, folder, downloadExtension
                    ).getOrElse {
                        errors.add(it)
                        null
                    }
                }
            }.awaitAll()
        dao.insertTrackEntity(
            trackEntity.copy(toMergeFiles = mergeFiles.mapNotNull { it?.absolutePath }.toJson())
        )
    }

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
}