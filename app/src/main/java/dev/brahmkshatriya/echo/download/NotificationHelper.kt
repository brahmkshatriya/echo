package dev.brahmkshatriya.echo.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import dev.brahmkshatriya.echo.R

object NotificationHelper {
    private const val CHANNEL_ID = "download_channel"
    private const val CHANNEL_NAME = "Downloads"
    private const val CHANNEL_DESCRIPTION = "Notifications for download status"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                    description = CHANNEL_DESCRIPTION
                }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(
        context: Context,
        title: String,
        progress: Int = 0,
        indeterminate: Boolean = false
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_downloading)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progress, indeterminate)
    }

    fun updateNotification(
        context: Context,
        downloadId: Int,
        builder: NotificationCompat.Builder
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(downloadId, builder.build())
    }

    fun completeNotification(context: Context, downloadId: Int, title: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_for_offline)
            .setContentTitle(title)
            .setContentText("Download complete")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(downloadId, builder.build())
    }

    fun errorNotification(context: Context, downloadId: Int, title: String, error: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_for_offline)
            .setContentTitle(title)
            .setContentText("Download failed: $error")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(downloadId, builder.build())
    }
}