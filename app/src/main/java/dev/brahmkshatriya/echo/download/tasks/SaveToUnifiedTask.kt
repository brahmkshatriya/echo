package dev.brahmkshatriya.echo.download.tasks

import android.content.Context
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.download.db.models.TaskType
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension.Companion.withExtensionId
import dev.brahmkshatriya.echo.utils.Serializer.toJson
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadDrawable

class SaveToUnifiedTask(
    private val app: Context,
    downloader: Downloader,
    override val trackId: Long,
) : BaseTask(app, downloader, trackId) {

    override val type = TaskType.Saving

    override suspend fun work(trackId: Long) {
        val old = getDownload()
        if (old.finalFile == null) return

        val download = old.copy(
            data = old.track.withExtensionId(old.extensionId).toJson(),
        )
        dao.insertDownloadEntity(download)

        val downloadContext = getDownloadContext()
        val context = downloadContext.context
        val allDownloads = dao.getDownloadsForContext(download.contextId)

        val unifiedExtension =
            downloader.extensionLoader.music.getExtensionOrThrow(UnifiedExtension.metadata.id)

        if (context != null) unifiedExtension.get<PlaylistEditClient, Unit> {
            val db = (this as UnifiedExtension).db
            val playlist = db.getOrCreate(app, context)
            val tracks = loadTracks(playlist).loadAll()
            if (allDownloads.all { it.finalFile != null }) {
                removeTracksFromPlaylist(playlist, tracks, tracks.indices.toList())
                val sorted = allDownloads.sortedBy { it.sortOrder }
                addTracksToPlaylist(playlist, listOf(), 0, sorted.map { it.track })
            } else addTracksToPlaylist(
                playlist, tracks, tracks.size, listOf(download.track)
            )
        }.getOrThrow()

        dao.insertDownloadEntity(download.copy(fullyDownloaded = true))

        val item = if (context == null) download.track.toMediaItem() else {
            if (allDownloads.all { it.finalFile != null }) context else null
        } ?: return
        createCompleteNotification(app, item.title, item.cover.loadDrawable(app))
    }
}