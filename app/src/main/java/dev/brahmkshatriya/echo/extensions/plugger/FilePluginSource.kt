package dev.brahmkshatriya.echo.extensions.plugger

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tel.jeelpa.plugger.PluginSource
import java.io.File

class FilePluginSource(
    private val folder: File,
    scope: CoroutineScope,
    fileIgnoreFlow: MutableSharedFlow<File?>
) : PluginSource<File> {

    private var ignoreFile : File? = null
    private fun loadAllPlugins() = run {
        folder.setReadOnly()
        folder.listFiles()!!.filter { it != ignoreFile }.onEach { it.setWritable(false) }
    }
    private val pluginStateFlow = MutableStateFlow(loadAllPlugins())

    init {
        scope.launch {
            fileIgnoreFlow.collect {
                ignoreFile = it
                pluginStateFlow.value = loadAllPlugins()
            }
        }
    }

    override fun getSourceFiles() = pluginStateFlow.asStateFlow()
}