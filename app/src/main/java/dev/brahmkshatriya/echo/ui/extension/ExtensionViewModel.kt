package dev.brahmkshatriya.echo.ui.extension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.di.MutableExtensionFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExtensionViewModel @Inject constructor(
    val mutableExtensionFlow: MutableExtensionFlow
) : ViewModel() {
    var extensionListFlow: Flow<List<ExtensionClient>>? = null
        set(value) {
            field = value
            viewModelScope.launch {
                mutableExtensionFlow.flow.value = value?.first()?.firstOrNull()
            }
        }
    fun setExtension(extension: ExtensionClient) {
        viewModelScope.launch {
            mutableExtensionFlow.flow.value = extension
        }
    }
}