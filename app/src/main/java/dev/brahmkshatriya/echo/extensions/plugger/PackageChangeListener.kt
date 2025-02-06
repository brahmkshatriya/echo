package dev.brahmkshatriya.echo.extensions.plugger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import java.util.Collections.synchronizedList

class PackageChangeListener(context: Context) : BroadcastReceiver() {

    fun add(listener: Listener) {
        listeners.add(listener)
    }
    private val listeners = synchronizedList(mutableListOf<Listener>())

    interface Listener {
        fun onPackageChanged()
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
        listeners.forEach { it.onPackageChanged() }
    }
}