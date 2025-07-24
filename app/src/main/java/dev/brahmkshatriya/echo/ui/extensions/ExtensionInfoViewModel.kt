package dev.brahmkshatriya.echo.ui.extensions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.SettingsChangeListenerClient
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.providers.SettingsProvider
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getIf
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.runIf
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getOrThrow
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ExtensionInfoViewModel(
    val app: App,
    val extensionLoader: ExtensionLoader,
    val type: ExtensionType,
    val id: String,
) : ViewModel() {

    private val reload = MutableSharedFlow<Unit>(1).also {
        viewModelScope.launch { it.emit(Unit) }
    }
    val extension = extensionLoader.getFlow(type)
        .combine(reload) { a, _ -> a }
        .map { list ->
            val ext = list.find { it.id == id }
            Triple(
                ext,
                ext?.isClient<LoginClient>() == true,
                ext?.getIf<SettingsProvider, List<Setting>>(app.throwFlow) {
                    getSettingItems()
                }.orEmpty()
            )
        }

    fun onSettingsChanged(settings: Settings, key: String?) = viewModelScope.launch {
        extension.first().first?.runIf<SettingsChangeListenerClient>(app.throwFlow) {
            onSettingsChanged(settings, key)
        }
    }

    fun onSettingsClick(onClick: suspend () -> Unit) = viewModelScope.launch {
        extension.first().first?.get { onClick() }?.getOrThrow(app.throwFlow)
    }
}