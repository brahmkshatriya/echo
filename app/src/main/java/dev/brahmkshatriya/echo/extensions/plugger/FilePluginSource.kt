package dev.brahmkshatriya.echo.extensions.plugger

import android.os.Build
import android.os.FileObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tel.jeelpa.plugger.PluginSource
import java.io.File

class FilePluginSource(
    private val folder: File,
    private val extension: String,
) : PluginSource<File> {
    private fun loadAllPlugins() = folder.listFiles()!!.filter { it.path.endsWith(extension) }
    private val pluginStateFlow = MutableStateFlow(loadAllPlugins())

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fsEventsListener = object : FileObserver(folder) {
                override fun onEvent(event: Int, path: String?) {
                    pluginStateFlow.value = loadAllPlugins()
                }
            }
            fsEventsListener.startWatching()
        }
    }

    override fun getSourceFiles() = pluginStateFlow.asStateFlow()
}