package dev.brahmkshatriya.echo.download.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.NotificationUtil.createNotificationChannel
import androidx.media3.common.util.UnstableApi
import androidx.work.ForegroundInfo
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.db.DownloadDao
import dev.brahmkshatriya.echo.db.models.MediaTaskEntity
import dev.brahmkshatriya.echo.db.models.Status
import kotlin.math.max

object NotificationUtil {
    suspend fun create(
        context: Context,
        dao: DownloadDao,
        mediaTaskEntities: List<MediaTaskEntity>
    ): ForegroundInfo? {
        val notificationId = 0
        val progressing = mediaTaskEntities.filter { it.status == Status.Progressing }
        if (progressing.isEmpty()) {
            removeNotification(context)
            return null
        }
        val progress = progressing.sumOf { it.progress } / progressing.size
        val speed = progressing.sumOf { it.speed ?: 0 } / progressing.size
        val total = progressing.sumOf { it.size ?: 0 } / progressing.size
        val title = if (progressing.size == 1) {
            progressing.first().run {
                title ?: dao.getTrackEntity(trackId).track.title
            }
        } else {
            context.getString(R.string.items, progressing.size)
        }
        return createNotification(context, notificationId, title, total, progress, speed)
    }

    private const val CHANNEL_ID = "download_channel"
    const val ACTION_PAUSE_ALL = "PAUSE_ALL"
    const val ACTION_CANCEL_ALL = "CANCEL_ALL"

    @OptIn(UnstableApi::class)
    private fun createNotification(
        context: Context,
        notificationId: Int,
        title: String,
        total: Long,
        progress: Long,
        speed: Long
    ): ForegroundInfo {
        createNotificationChannel(
            context, CHANNEL_ID, R.string.downloads, 0,
            NotificationUtil.IMPORTANCE_DEFAULT
        )
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
        val max = max(total, progress)
        val p = if (max > 0) (progress * 100 / max).toInt() else 0
        val indeterminate = total == 0L
        val sub = buildString {
            append(context.getString(R.string.progress_x, p))
            if (speed > 0) {
                append(" • ")
                append(convertSpeed(speed))
            }
        }
        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                putExtra("fromDownload", true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val pendingIntentPause = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_PAUSE_ALL
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val pendingIntentCancel = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_CANCEL_ALL
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return ForegroundInfo(
            notificationId,
            notificationBuilder
                .setSmallIcon(R.drawable.ic_downloading)
                .setContentTitle(context.getString(R.string.downloading_x, title))
                .setSubText(sub)
                .setContentIntent(intent)
                .setProgress(100, p, indeterminate)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .addAction(-1, context.getString(R.string.pause_all), pendingIntentPause)
                .addAction(-1, context.getString(R.string.cancel_all), pendingIntentCancel)
                .build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )
    }

    private fun removeNotification(context: Context, notificationId: Int = 0) {
        NotificationManagerCompat.from(context).cancel(notificationId)
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

}