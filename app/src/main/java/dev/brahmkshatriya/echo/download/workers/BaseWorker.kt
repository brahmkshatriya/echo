package dev.brahmkshatriya.echo.download.workers

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.NotificationUtil.createNotificationChannel
import androidx.media3.common.util.UnstableApi
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.DownloadClient
import dev.brahmkshatriya.echo.common.models.DownloadContext
import dev.brahmkshatriya.echo.common.models.Progress
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.download.db.models.DownloadEntity
import dev.brahmkshatriya.echo.download.db.models.TaskType
import dev.brahmkshatriya.echo.download.exceptions.DownloadException
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.ui.common.ExceptionUtils.toData
import dev.brahmkshatriya.echo.utils.CoroutineUtils.throttleLatest
import dev.brahmkshatriya.echo.utils.Serializer.rootCause
import dev.brahmkshatriya.echo.utils.Serializer.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

abstract class BaseWorker(
    private val context: Context,
    workerParams: WorkerParameters,
    val downloader: Downloader,
) : CoroutineWorker(context, workerParams) {

    abstract val type: TaskType
    val progressFlow = MutableStateFlow(Progress())
    suspend fun <T> withDownloadExtension(block: suspend DownloadClient.() -> T) =
        downloader.downloadExtension().get<DownloadClient, T> { block() }.getOrThrow()

    val dao = downloader.dao

    override suspend fun doWork() = withContext(Dispatchers.IO) {
        permit {
            val job = launch {
                val download = getDownload()
                progressFlow.throttleLatest(1000L).collectLatest { update(it, download) }
            }
            runCatching {
                val trackId = inputData.getLong("id", -1).takeIf { it != -1L }!!
                work(trackId)
                job.cancel()
                removeNotification()
                Result.success()
            }.getOrElse { throwable ->
                val trackId = inputData.getLong("id", -1).takeIf { it != -1L }
                    ?: return@getOrElse Result.failure()
                val download = dao.getDownloadEntity(trackId) ?: return@getOrElse Result.failure()
                val exception = DownloadException(TaskType.Loading, download, throwable)
                dao.insertDownloadEntity(
                    download.copy(exceptionData = exception.rootCause.toData(context).toJson())
                )
                Result.failure()
            }
        }
    }

    suspend fun getDownload(): DownloadEntity {
        val trackId = inputData.getLong("id", -1).takeIf { it != -1L }!!
        return dao.getDownloadEntity(trackId)!!
    }

    suspend fun getDownloadContext() = run {
        val download = getDownload()
        val contextEntity = download.contextId?.let { dao.getContextEntity(it) }
        DownloadContext(
            download.extensionId, download.track, download.sortOrder, contextEntity?.mediaItem
        )
    }

    open suspend fun <T> permit(block: suspend () -> T): T = block()
    abstract suspend fun work(trackId: Long)

    private suspend fun update(progress: Progress, download: DownloadEntity) {
        setProgress(progress.toData(type))
        setForeground(
            createNotification(
                context,
                download.track.title,
                progress.size,
                progress.progress,
                progress.speed
            )
        )
    }

    companion object {
        fun createInputData(id: Long): Data {
            return Data.Builder()
                .putLong("id", id)
                .build()
        }

        fun Progress.toData(type: TaskType) = Data.Builder()
            .putString("type", type.name)
            .putLong("progress", progress)
            .putLong("size", size)
            .putLong("speed", speed)
            .build()

        fun Data.toProgress() = TaskType.valueOf(getString("type")!!) to Progress(
            getLong("size", 0),
            getLong("progress", 0),
            getLong("speed", 0)
        )

        private const val PROGRESS_CHANNEL_ID = "download_progress_channel"

        fun Context.getTitle(type: TaskType, title: String) = when (type) {
            TaskType.Loading -> getString(R.string.loading_x, title)
            TaskType.Downloading -> getString(R.string.downloading_x, title)
            TaskType.Merging -> getString(R.string.merging_x, title)
            TaskType.Tagging -> getString(R.string.tagging_x, title)
        }

        private const val DOWNLOAD_CHANNEL_ID = "download_channel"
    }

    private fun getMainIntent(context: Context) = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            putExtra("fromDownload", true)
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    @OptIn(UnstableApi::class)
    private fun createNotification(
        context: Context,
        title: String,
        total: Long,
        progress: Long,
        speed: Long
    ): ForegroundInfo {
        createNotificationChannel(
            context, PROGRESS_CHANNEL_ID, R.string.download_progress, 0,
            NotificationUtil.IMPORTANCE_DEFAULT
        )
        val notificationBuilder = NotificationCompat.Builder(context, PROGRESS_CHANNEL_ID)
        val max = max(total, progress)
        val p = if (max > 0) (progress * 100 / max).toInt() else 0
        val indeterminate = total == 0L
        val sub = buildString {
            append(context.getString(R.string.progress_x_percentage, p))
            if (speed > 0) {
                append(" • ")
                append(convertSpeed(speed))
            }
        }
        val intent = getMainIntent(context)

        val pendingIntentCancel = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)

        return ForegroundInfo(
            hashCode(),
            notificationBuilder
                .setSmallIcon(R.drawable.ic_downloading)
                .setContentTitle(context.getTitle(type, title))
                .setSubText(sub)
                .setContentIntent(intent)
                .setProgress(100, p, indeterminate)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .addAction(-1, context.getString(R.string.cancel), pendingIntentCancel)
                .build(),
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) 0
            else FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun removeNotification() {
        NotificationManagerCompat.from(context).cancel(hashCode())
    }

    private fun convertSpeed(speedInBPerMs: Long): String {
        var value = speedInBPerMs.toFloat() * 1000
        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
        var unitIndex = 0

        while (value >= 500 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }

        return "%.2f %s".format(value, units[unitIndex])
    }

    @OptIn(UnstableApi::class)
    fun createCompleteNotification(
        context: Context,
        title: String,
        drawable: Drawable?
    ) {
        createNotificationChannel(
            context, DOWNLOAD_CHANNEL_ID, R.string.download_complete, 0,
            NotificationUtil.IMPORTANCE_DEFAULT
        )
        val notificationBuilder = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_downloading)
            .setContentTitle(context.getString(R.string.download_complete))
            .setContentText(title)
            .setStyle(
                NotificationCompat.BigPictureStyle().bigPicture(drawable?.toBitmap())
            )
            .setContentIntent(getMainIntent(context))
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(context, POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
            return
        }
        NotificationManagerCompat.from(context).notify(
            title.hashCode(),
            notificationBuilder.build()
        )
    }
}