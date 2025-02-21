package dev.brahmkshatriya.echo.playback.listener

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSessionService.Listener
import dev.brahmkshatriya.echo.R

@UnstableApi
class MediaSessionServiceListener(
    private val context: Context,
    private val intent: PendingIntent
) : Listener {

    override fun onForegroundServiceStartNotAllowedException() {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val notificationManagerCompat = NotificationManagerCompat.from(context)
        context.ensureNotificationChannel(notificationManagerCompat)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mono)
            .setContentTitle(context.getString(R.string.app_name))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(context.getString(R.string.app_name))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(intent)
        notificationManagerCompat.notify(NOTIFICATION_ID, builder.build())
    }

    private fun Context.ensureNotificationChannel(
        notificationManagerCompat: NotificationManagerCompat
    ) {
        if (
            Build.VERSION.SDK_INT < 26 ||
            notificationManagerCompat.getNotificationChannel(CHANNEL_ID) != null
        ) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManagerCompat.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "echo_notification_channel_id"
        const val NOTIFICATION_ID = 1
    }
}