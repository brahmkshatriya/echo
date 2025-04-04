package dev.brahmkshatriya.echo.download

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.common.models.Progress
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.download.db.DownloadDatabase
import dev.brahmkshatriya.echo.download.db.models.ContextEntity
import dev.brahmkshatriya.echo.download.db.models.DownloadEntity
import dev.brahmkshatriya.echo.download.db.models.TaskType
import dev.brahmkshatriya.echo.download.exceptions.DownloaderExtensionNotFoundException
import dev.brahmkshatriya.echo.download.workers.BaseWorker.Companion.createInputData
import dev.brahmkshatriya.echo.download.workers.BaseWorker.Companion.toProgress
import dev.brahmkshatriya.echo.download.workers.LoadingWorker
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.await
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.utils.Serializer.toJson
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.WeakHashMap
import java.util.concurrent.ThreadPoolExecutor

class Downloader(
    val app: App,
    database: DownloadDatabase,
    extensionLoader: ExtensionLoader,
    private val executor: ThreadPoolExecutor,
) {
    suspend fun downloadExtension() = extensions.misc.await()
        .find { it.isClient<DownloadClient>() && it.isEnabled }
        ?: throw DownloaderExtensionNotFoundException()

    val scope = CoroutineScope(Dispatchers.IO) + CoroutineName("Downloader")

    val extensions = extensionLoader.extensions
    val dao = database.downloadDao()

    fun add(
        downloads: List<DownloadContext>
    ) = scope.launch {
        val concurrentDownloads =
            downloadExtension().get<DownloadClient, Int> { concurrentDownloads }.getOrNull() ?: 2
        executor.maximumPoolSize = concurrentDownloads
        executor.corePoolSize = concurrentDownloads

        val contexts = downloads.mapNotNull { it.context }.distinctBy { it.id }.associate {
            it.id to dao.insertContextEntity(ContextEntity(0, it.toJson()))
        }
        val ids = downloads.map {
            dao.insertDownloadEntity(
                DownloadEntity(
                    0, it.extensionId, contexts[it.context?.id], it.track.toJson(), it.sortOrder
                )
            )
        }
        ids.forEach { startDownload(it) }
    }

    val workManager by lazy { WorkManager.getInstance(app.context) }
    private fun startDownload(id: Long) {
        val request = OneTimeWorkRequestBuilder<LoadingWorker>()
            .setInputData(createInputData(id))
            .addTag(id.toString())
            .build()
        workManager.beginUniqueWork(id.toString(), ExistingWorkPolicy.REPLACE, request)
            .enqueue()
    }

    private val servers = WeakHashMap<Long, Streamable.Media.Server>()
    private val mutexes = WeakHashMap<Long, Mutex>()

    suspend fun getServer(
        trackId: Long, download: DownloadEntity
    ): Streamable.Media.Server = mutexes.getOrPut(trackId) { Mutex() }.withLock {
        servers.getOrPut(trackId) {
            val extensionId = download.extensionId
            val extension = extensions.music.getExtensionOrThrow(extensionId)
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
        workManager.cancelUniqueWork(trackId.toString())
        scope.launch {
            val entity = dao.getDownloadEntity(trackId) ?: return@launch
            dao.deleteDownloadEntity(entity)
            servers.remove(trackId)
            mutexes.remove(trackId)
        }
    }

    fun restart(trackId: Long) {
        workManager.cancelUniqueWork(trackId.toString())
        startDownload(trackId)
    }

    fun cancelAll() {
        scope.launch {
            val downloads = dao.getDownloadsFlow().first().filter { it.finalFile == null }
            downloads.forEach { download ->
                workManager.cancelUniqueWork(download.id.toString())
                dao.deleteDownloadEntity(download)
                servers.remove(download.id)
                mutexes.remove(download.id)
            }
        }
    }

    data class Info(
        val download: DownloadEntity,
        val context: ContextEntity?,
        val workers: List<Pair<TaskType, Progress>>
    )

    val flow = dao.run {
        val workFlow = workManager.getWorkInfosFlow(
            WorkQuery.fromStates(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING)
        )
        getDownloadsFlow().combine(getContextFlow()) { downloads, contexts ->
            downloads.map { download ->
                val context = contexts.find { download.contextId == it.id }
                download to context
            }
        }.combine(workFlow) { downloads, infos ->
            downloads.map { (download, context) ->
                Info(
                    download, context,
                    infos.filter { it.tags.contains(download.id.toString()) }
                        .mapNotNull { runCatching { it.progress.toProgress() }.getOrNull() }
                )
            }.sortedByDescending { it.workers.size }
        }
    }.flowOn(Dispatchers.IO)

}