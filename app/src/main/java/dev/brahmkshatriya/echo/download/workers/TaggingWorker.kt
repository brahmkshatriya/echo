package dev.brahmkshatriya.echo.download.workers

import android.content.Context
import android.media.MediaScannerConnection
import androidx.work.WorkerParameters
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.download.db.models.TaskType
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
    }

}