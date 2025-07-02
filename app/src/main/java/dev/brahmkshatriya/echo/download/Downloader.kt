package dev.brahmkshatriya.echo.download

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Progress
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.download.db.DownloadDatabase
import dev.brahmkshatriya.echo.download.db.models.ContextEntity
import dev.brahmkshatriya.echo.download.db.models.DownloadEntity
import dev.brahmkshatriya.echo.download.db.models.TaskType
import dev.brahmkshatriya.echo.download.exceptions.DownloaderExtensionNotFoundException
import dev.brahmkshatriya.echo.download.tasks.TaskManager
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension.Companion.EXTENSION_ID
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension.Companion.withExtensionId
import dev.brahmkshatriya.echo.utils.Serializer.toJson
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.WeakHashMap

class Downloader(
    val app: App,
    val downloadShelf: MutableStateFlow<List<Shelf>>,
    val extensionLoader: ExtensionLoader,
    database: DownloadDatabase,
) {

    suspend fun downloadExtension() = extensionLoader.misc.value
        .find { it.isClient<DownloadClient>() && it.isEnabled }
        ?: throw DownloaderExtensionNotFoundException()

    val scope = CoroutineScope(Dispatchers.IO) + CoroutineName("Downloader")

    val dao = database.downloadDao()
    val downloadFlow = dao.getDownloadsFlow()
    private val contextFlow = dao.getContextFlow()
    private val downloadsFlow = downloadFlow.combine(contextFlow) { downloads, contexts ->
        downloads.map { download ->
            val context = contexts.find { download.contextId == it.id }
            Info(download, context, listOf())
        }
    }

    val taskManager = TaskManager(this)

    fun add(
        downloads: List<DownloadContext>
    ) = scope.launch {
        val concurrentDownloads = downloadExtension()
            .get<DownloadClient, Int> { concurrentDownloads }
            .getOrNull()?.takeIf { it > 0 } ?: 2
        taskManager.setConcurrency(concurrentDownloads)
        val contexts = downloads.mapNotNull { it.context }.distinctBy { it.id }.associate {
            it.id to dao.insertContextEntity(ContextEntity(0, it.id, it.toJson()))
        }
        downloads.forEach {
            dao.insertDownloadEntity(
                DownloadEntity(
                    0,
                    it.track.extras[EXTENSION_ID] ?: it.extensionId,
                    it.track.id,
                    contexts[it.context?.id],
                    it.sortOrder,
                    it.track.toJson(),
                    TaskType.Loading,
                )
            )
        }
        ensureWorker()
    }

    private val workManager by lazy { WorkManager.getInstance(app.context) }
    private fun ensureWorker() {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(Constraints(NetworkType.CONNECTED, requiresStorageNotLow = true))
            .addTag(TAG)
            .build()
        workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request)
    }

    private val servers = WeakHashMap<Long, Streamable.Media.Server>()
    private val mutexes = WeakHashMap<Long, Mutex>()

    suspend fun getServer(
        trackId: Long, download: DownloadEntity
    ): Streamable.Media.Server = mutexes.getOrPut(trackId) { Mutex() }.withLock {
        servers.getOrPut(trackId) {
            val extensionId = download.extensionId
            val extension = extensionLoader.music.getExtensionOrThrow(extensionId)
            val streamable = download.track.streamables.find { it.id == download.streamableId }!!
            extension.get<TrackClient, Streamable.Media.Server> {
                val media =
                    loadStreamableMedia(streamable, true) as Streamable.Media.Server
                media.sources.ifEmpty {
                    throw Exception("${trackId}: No sources found")
                }
                media
            }.getOrThrow()
        }
    }

    fun cancel(trackId: Long) {
        taskManager.remove(trackId)
        scope.launch {
            val entity = dao.getDownloadEntity(trackId) ?: return@launch
            dao.deleteDownloadEntity(entity)
            entity.exceptionFile?.let {
                val file = File(it)
                if (file.exists()) file.delete()
            }
            servers.remove(trackId)
            mutexes.remove(trackId)
        }
    }

    fun restart(trackId: Long) {
        taskManager.remove(trackId)
        scope.launch {
            val download = dao.getDownloadEntity(trackId) ?: return@launch
            dao.insertDownloadEntity(
                download.copy(exceptionFile = null, finalFile = null, fullyDownloaded = false)
            )
            download.exceptionFile?.let {
                val file = File(it)
                if (file.exists()) file.delete()
            }
            servers.remove(trackId)
            mutexes.remove(trackId)
            ensureWorker()
        }
    }

    fun cancelAll() {
        taskManager.removeAll()
        scope.launch {
            val downloads = downloadFlow.first().filter { it.finalFile == null }
            downloads.forEach { download ->
                dao.deleteDownloadEntity(download)
                servers.remove(download.id)
                mutexes.remove(download.id)
            }
        }
    }

    fun deleteDownload(id: String) {
        scope.launch {
            val downloads = downloadFlow.first().filter { it.trackId == id }
            downloads.forEach { download ->
                dao.deleteDownloadEntity(download)
            }
        }
    }

    fun deleteContext(id: String) {
        scope.launch {
            val contexts = contextFlow.first().filter { it.itemId == id }
            contexts.forEach { context ->
                dao.deleteContextEntity(context)
                val downloads = downloadFlow.first().filter {
                    it.contextId == context.id
                }
                downloads.forEach { download ->
                    dao.deleteDownloadEntity(download)
                }
            }
        }
    }

    data class Info(
        val download: DownloadEntity,
        val context: ContextEntity?,
        val workers: List<Pair<TaskType, Progress>>
    )

    val flow = downloadsFlow.combine(taskManager.progressFlow) { downloads, info ->
        downloads.map { (dl, context) ->
            val workers = info.filter { it.first.trackId == dl.id }.map { (a, b) -> a.type to b }
            Info(dl, context, workers)
        }.sortedByDescending { it.workers.size }
    }.stateIn(scope, SharingStarted.Eagerly, listOf())

    init {
        scope.launch {
            downloadsFlow.map { info ->
                val unifiedExtension = extensionLoader.unified.value as UnifiedExtension

                info.filter { it.download.fullyDownloaded }.groupBy {
                    it.context?.id
                }.flatMap { (id, infos) ->
                    if (id == null) infos.map {
                        it.download.track.toMediaItem().toShelf()
                            .withExtensionId(it.download.extensionId)
                    }
                    else listOfNotNull(infos.first().run {
                        unifiedExtension.db.getPlaylist(context!!.mediaItem)?.toMediaItem()
                            ?.toShelf()
                    })
                }
            }.collect(downloadShelf)
        }
    }

    companion object {
        private const val TAG = "Downloader"
    }
}