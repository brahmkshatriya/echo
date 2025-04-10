package dev.brahmkshatriya.echo.download.workers

import android.content.Context
import android.media.MediaScannerConnection
import androidx.work.WorkerParameters
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.download.db.models.TaskType
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadDrawable
import java.io.File

class TaggingWorker(
    context: Context,
    workerParams: WorkerParameters,
    downloader: Downloader,
) : BaseWorker(context, workerParams, downloader) {

    override val type = TaskType.Tagging

    override suspend fun work(trackId: Long) {
        val downloadContext = getDownloadContext()
        val download = getDownload()
        val file = withDownloadExtension {
            tag(progressFlow, downloadContext, File(download.toTagFile!!))
        }
        dao.insertDownloadEntity(download.copy(finalFile = file.toString()))
        MediaScannerConnection.scanFile(
            applicationContext, arrayOf(file.toString()), null, null
        )
        val context = downloadContext.context
        val item = if (context == null) download.track.toMediaItem() else {
            val allDownloads = dao.getDownloadsForContext(context.id)
            if (allDownloads.all { it.finalFile != null }) context else null
        } ?: return
        createCompleteNotification(
            applicationContext, item.title, item.cover.loadDrawable(applicationContext)
        )
    }

}