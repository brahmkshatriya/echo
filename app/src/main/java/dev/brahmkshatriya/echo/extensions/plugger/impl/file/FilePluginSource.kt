package dev.brahmkshatriya.echo.extensions.plugger.impl.file

import android.content.Context
import dev.brahmkshatriya.echo.extensions.plugger.ExtensionsRepo.Companion.getPluginFileDir
import dev.brahmkshatriya.echo.extensions.plugger.interfaces.PluginSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class FilePluginSource(
    context: Context,
    scope: CoroutineScope,
) : PluginSource<File> {

    private val folder = context.getPluginFileDir()
    private var ignoreFile: File? = null
    val fileIgnoreFlow = MutableSharedFlow<File?>()

    private fun loadAllPlugins() = run {
        folder.setReadOnly()
        folder.listFiles()!!.filter {
            it != ignoreFile && it.extension == "apk"
        }.onEach { it.setWritable(false) }
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