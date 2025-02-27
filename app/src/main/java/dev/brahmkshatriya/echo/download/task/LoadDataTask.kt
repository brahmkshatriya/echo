package dev.brahmkshatriya.echo.download.task

import dev.brahmkshatriya.echo.common.MiscExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.helpers.Progress
import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.download.db.DownloadDao
import dev.brahmkshatriya.echo.download.db.models.EchoMediaItemEntity
import dev.brahmkshatriya.echo.download.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.download.db.models.TaskType
import dev.brahmkshatriya.echo.download.db.models.TrackDownloadTaskEntity
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.utils.Serializer.toJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

typealias MediaProgress = Progress<Streamable.Media.Server>

class LoadDataTask(
    scope: CoroutineScope,
    private val dao: DownloadDao,
    private var trackEntity: TrackDownloadTaskEntity,
    private var context: EchoMediaItemEntity?,
    private val extensionsList: MutableStateFlow<List<MusicExtension>?>,
    private val downloadExtension: MiscExtension,
) : MediaTask<Streamable.Media.Server>(dao, scope) {

    override val entity =
        MediaTaskEntity(trackEntity.id, trackEntity.id, TaskType.METADATA, null, true)

    private var progress = 0L
        set(value) {
            field = value
            runBlocking { progressFlow.emit(Progress.InProgress(value, null)) }
        }

    private suspend fun load(): Streamable.Media.Server {
        progress = 0

        val extension = extensionsList.getExtensionOrThrow(trackEntity.extensionId)
        trackEntity = dao.getTrackEntity(trackEntity.id) ?: throw Exception("Track not found")
        val streamables = if (!trackEntity.loaded) {
            val (track, streamables) =
                extension.get<TrackClient, Pair<Track, List<Streamable>>> {
                    val track = loadTrack(trackEntity.track)
                    track to track.servers.ifEmpty {
                        throw Exception("${track.title}: No servers found")
                    }
                }.getOrThrow()

            trackEntity = trackEntity.copy(data = track.toJson(), loaded = true)
            dao.insertTrackEntity(trackEntity)
            streamables
        } else trackEntity.track.streamables
        progress++

        val downloadContext = DownloadContext(
            trackEntity.extensionId, trackEntity.track, trackEntity.sortOrder, context?.mediaItem
        )
        val folderPath = trackEntity.folderPath
        if (folderPath == null) {
            val file = withDownloadExtension { getDownloadDir(downloadContext) }
            trackEntity = trackEntity.copy(folderPath = file.absolutePath)
            dao.insertTrackEntity(trackEntity)
        } else File(folderPath)
        progress++

        val streamableId = trackEntity.streamableId
        val selectedStreamable = if (streamableId == null) {
            val selected = withDownloadExtension { selectServer(downloadContext) }
            trackEntity = trackEntity.copy(streamableId = selected.id)
            dao.insertTrackEntity(trackEntity)
            selected
        } else streamables.find { it.id == streamableId }!!
        progress++

        val server = extension.get<TrackClient, Streamable.Media.Server> {
            val media =
                loadStreamableMedia(selectedStreamable, true) as Streamable.Media.Server
            media.sources.ifEmpty {
                throw Exception("${trackEntity.track.title}: No sources found")
            }
            media
        }.getOrThrow()

        val indexes = trackEntity.indexes.ifEmpty {
            val sources = withDownloadExtension { selectSources(downloadContext, server) }
            sources.map { server.sources.indexOf(it) }
        }
        if (indexes.isEmpty()) throw Exception("No files to download")
        trackEntity = trackEntity.copy(indexesData = indexes.toJson())
        dao.insertTrackEntity(trackEntity)
        progress++

        return server
    }

    private suspend fun <T> withDownloadExtension(block: suspend DownloadClient.() -> T) =
        downloadExtension.get<DownloadClient, T> { block() }.getOrThrow()

    var job: Job? = null
    private val progressFlow = MutableSharedFlow<MediaProgress>()
    override suspend fun initialize() = progressFlow

    override suspend fun start() = coroutineScope {
        job = launch {
            progressFlow.emit(Progress.Initialized(4))
            val final = runCatching { load() }.getOrElse {
                progressFlow.emit(Progress.Final.Failed(it))
                return@launch
            }
            progressFlow.emit(Progress.Final.Completed(4, final))
        }
    }

    override suspend fun cancel() {
        progressFlow.emit(Progress.Final.Cancelled())
        runCatching { job?.cancel() }
        job = null
    }

    override suspend fun pause() {
        progressFlow.emit(Progress.Paused(progress))
        runCatching { job?.cancel() }
        job = null
    }

    override suspend fun resume() {
        start()
    }

}
