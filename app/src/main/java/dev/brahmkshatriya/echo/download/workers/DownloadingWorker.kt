package dev.brahmkshatriya.echo.download.workers

import android.content.Context
import androidx.work.Data
import androidx.work.WorkerParameters
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.download.db.models.TaskType
import dev.brahmkshatriya.echo.utils.Serializer.toJson

class DownloadingWorker(
    context: Context,
    workerParams: WorkerParameters,
    downloader: Downloader,
) : BaseWorker(context, workerParams, downloader) {

    override val type = TaskType.Downloading
    override suspend fun work(trackId: Long) {
        var download = getDownload()
        val index = inputData.getInt("index", -1).takeIf { it != -1 }!!
        val server = downloader.getServer(trackId, download)
        val source = server.sources[index]
        val downloadContext = getDownloadContext()
        val file = withDownloadExtension { download(progressFlow, downloadContext, source) }
        download = getDownload()
        download =
            download.copy(toMergeFilesData = (download.toMergeFiles + file.toString()).toJson())
        dao.insertDownloadEntity(download)
    }

    companion object {
        fun createInputData(trackId: Long, index: Int): Data {
            return Data.Builder()
                .putLong("id", trackId)
                .putInt("index", index)
                .build()
        }
    }
}