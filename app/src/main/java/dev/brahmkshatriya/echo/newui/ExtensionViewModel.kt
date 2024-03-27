package dev.brahmkshatriya.echo.newui

import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.ui.extension.ExtensionViewModel
import dev.brahmkshatriya.echo.utils.catchWith
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import tel.jeelpa.plugger.PluginRepo
import javax.inject.Inject

@HiltViewModel
class ExtensionViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    private val extensionListFlow: ExtensionModule.ExtensionListFlow,
    private val mutableExtensionFlow: ExtensionModule.MutableFlow,
    private val pluginRepo: PluginRepo<ExtensionClient>,
    private val preferences: SharedPreferences
) : CatchingViewModel(throwableFlow) {

    override fun onInitialize() {
        viewModelScope.launch {
            tryWith {
                pluginRepo.getAllPlugins {
                    viewModelScope.launch { throwableFlow.emit(it) }
                }.catchWith(throwableFlow).collect(extensionListFlow.flow)
            }
            extensionListFlow.flow.collectLatest { list ->
                list ?: return@collectLatest
                val id = preferences.getString(ExtensionViewModel.LAST_EXTENSION_KEY, null)
                mutableExtensionFlow.flow.value =
                    list.find { it.metadata.id == id } ?: list.firstOrNull()
            }
        }
    }
}