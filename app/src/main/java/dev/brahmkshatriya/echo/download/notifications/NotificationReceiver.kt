package dev.brahmkshatriya.echo.download.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.download.TaskAction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var actionFlow: MutableSharedFlow<TaskAction>

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        when (intent.action) {
            NotificationUtil.ACTION_PAUSE_ALL -> emit(TaskAction.All.PauseAll)
            NotificationUtil.ACTION_CANCEL_ALL -> emit(TaskAction.All.RemoveAll)
        }
    }

    fun emit(action: TaskAction) {
        runBlocking { actionFlow.emit(action) }
    }
}