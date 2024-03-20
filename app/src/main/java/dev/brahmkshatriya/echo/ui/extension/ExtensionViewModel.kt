package dev.brahmkshatriya.echo.ui.extension

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.utils.catchWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import tel.jeelpa.plugger.PluginRepo
import javax.inject.Inject

@HiltViewModel
class ExtensionViewModel @Inject constructor(
    private val mutableExtensionFlow: ExtensionModule.MutableFlow,
    private val pluginRepo: PluginRepo<ExtensionClient>,
    private val preferences: SharedPreferences,
    val throwableFlow: MutableSharedFlow<Throwable>
) : ViewModel() {

    val extensionFlow = mutableExtensionFlow.flow.asStateFlow()

    private var initialized = false
    fun initialize() {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            extensionListFlow = pluginRepo.getAllPlugins {
                launch { throwableFlow.emit(it) }
            }.catchWith(throwableFlow)
            val list = extensionListFlow?.firstOrNull()
            val id = preferences.getString(LAST_EXTENSION_KEY, null)
            mutableExtensionFlow.flow.value = list?.find {
                it.metadata.id == id
            } ?: list?.firstOrNull()
        }
    }

    private var extensionListFlow: Flow<List<ExtensionClient>>? = null
    fun getExtensionList() = extensionListFlow

    fun setExtension(extension: ExtensionClient) {
        preferences.edit().putString(LAST_EXTENSION_KEY, extension.metadata.id).apply()
        viewModelScope.launch {
            mutableExtensionFlow.flow.value = extension
        }
    }

    fun getCurrentExtension() = mutableExtensionFlow.flow.value

    companion object {
        const val LAST_EXTENSION_KEY = "last_extension"
    }
}