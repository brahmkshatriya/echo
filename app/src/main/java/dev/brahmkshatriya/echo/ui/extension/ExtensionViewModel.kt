package dev.brahmkshatriya.echo.ui.extension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.di.MutableExtensionFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import tel.jeelpa.plugger.PluginRepo
import javax.inject.Inject

@HiltViewModel
class ExtensionViewModel @Inject constructor(
    private val mutableExtensionFlow: MutableExtensionFlow,
    private val pluginRepo: PluginRepo<ExtensionClient>
) : ViewModel() {

    val extensionFlow = mutableExtensionFlow.flow.asStateFlow()
    private val _throwableFlow = MutableSharedFlow<Throwable>()
    val throwableFlow = _throwableFlow.asSharedFlow()
    init {
        viewModelScope.launch {
            extensionListFlow = pluginRepo.getAllPlugins { e ->
                launch {
                    _throwableFlow.emit(e)
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