package dev.brahmkshatriya.echo.widget

import android.graphics.Bitmap
import androidx.media3.session.MediaController
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.getController

object ControllerHelper {

    var controller: MediaController? = null
    var callback: (() -> Unit)? = null
    var image: Bitmap? = null
    var listener: WidgetPlayerListener? = null

    val map = mutableMapOf<String, () -> Unit>()
    private val pendingActions = mutableListOf<() -> Unit>()
    fun register(app: App, key: String, updateCallback: () -> Unit) {
        map[key] = updateCallback
        if (callback != null) return
        callback = getController(app.context) {
            controller = it
            val playerListener = WidgetPlayerListener { img ->
                image = img
                updateWidgets()
            }
            listener = playerListener
            it.addListener(playerListener)
            playerListener.controller = it
            runPendingActions()
            updateWidgets()
        }
    }

    fun unregister(key: String) {
        map.remove(key)
        if (map.isNotEmpty()) return
        callback?.invoke()
        controller = null
        listener?.removed()
        listener = null
        image = null
        pendingActions.clear()
    }

    // Widgets can be clicked before the media controller is connected,
    // queue the action and run it as soon as the controller is available
    fun whenReady(action: () -> Unit) {
        if (controller != null) action() else pendingActions.add(action)
    }

    private fun runPendingActions() {
        pendingActions.toList().forEach { it() }
        pendingActions.clear()
    }

    fun updateWidgets() {
        map.values.forEach { it() }
    }
}