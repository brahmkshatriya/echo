package dev.brahmkshatriya.echo.extensions.plugger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PackageChangeListener(
    context: Context,
    val scope: CoroutineScope
) : BroadcastReceiver() {

    fun add(listener: Listener) {
        listeners = listeners + listener
    }

    private var listeners = listOf<Listener>()

    interface Listener {
        suspend fun onPackageChanged()
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        context.registerReceiver(this, filter)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        listeners.forEach { scope.launch { it.onPackageChanged() } }
    }
}