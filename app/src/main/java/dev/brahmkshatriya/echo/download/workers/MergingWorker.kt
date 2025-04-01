package dev.brahmkshatriya.echo.download.workers

import android.content.Context
import androidx.work.WorkerParameters
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.download.db.models.TaskType
import java.io.File

class MergingWorker(
    context: Context,
    workerParams: WorkerParameters,
    downloader: Downloader,
) : BaseWorker(context, workerParams, downloader) {
    override val type = TaskType.Merging

    override suspend fun work(trackId: Long) {
        val downloadContext = getDownloadContext()
        val download = getDownload()
        val file = withDownloadExtension {
            merge(progressFlow, downloadContext, download.toMergeFiles.map { File(it) })
        }
        dao.insertDownloadEntity(download.copy(toTagFile = file.toString()))
    }

}