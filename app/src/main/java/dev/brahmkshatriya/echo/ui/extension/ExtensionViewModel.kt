package dev.brahmkshatriya.echo.ui.extension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.di.MutableExtensionFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import tel.jeelpa.plugger.PluginRepo
import javax.inject.Inject

@HiltViewModel
class ExtensionViewModel @Inject constructor(
    private val mutableExtensionFlow: MutableExtensionFlow,
    private val pluginRepo: PluginRepo<ExtensionClient>,
    val throwableFlow: MutableSharedFlow<Throwable>
) : ViewModel() {

    val extensionFlow = mutableExtensionFlow.flow.asStateFlow()

    private var initialized = false
    fun initialize() {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            extensionListFlow = pluginRepo.getAllPlugins { e ->
                launch {
                    throwableFlow.emit(e)
                }
            }
            mutableExtensionFlow.flow.value = extensionListFlow?.firstOrNull()?.firstOrNull()
        }
    }

    private var extensionListFlow: Flow<List<ExtensionClient>>? = null
    fun getExtensionList() = extensionListFlow

    fun setExtension(extension: ExtensionClient) {
        viewModelScope.launch {
            mutableExtensionFlow.flow.value = extension
        }
    }

    fun getCurrentExtension() = mutableExtensionFlow.flow.value
}