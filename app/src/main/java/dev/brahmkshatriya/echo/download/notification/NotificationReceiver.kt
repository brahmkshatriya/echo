package dev.brahmkshatriya.echo.download.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.brahmkshatriya.echo.download.Downloader
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotificationReceiver : BroadcastReceiver(), KoinComponent {

    private val downloader by inject<Downloader>()
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        downloader
    }
}