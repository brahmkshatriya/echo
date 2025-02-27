package dev.brahmkshatriya.echo.download.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.download.TaskAction
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotificationReceiver : BroadcastReceiver(), KoinComponent {

    private val downloader by inject<Downloader>()
    private val actions by lazy { downloader.actions }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        when (intent.action) {
            NotificationUtil.ACTION_CANCEL_ALL -> emit(TaskAction.RemoveAll)
        }
    }

    fun emit(action: TaskAction) {
        downloader.scope.launch { actions.emit(action) }
    }
}