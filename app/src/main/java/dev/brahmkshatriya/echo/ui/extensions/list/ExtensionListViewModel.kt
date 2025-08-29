package dev.brahmkshatriya.echo.ui.extensions.list

import androidx.lifecycle.ViewModel
import dev.brahmkshatriya.echo.common.Extension
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class ExtensionListViewModel<T : Extension<*>> : ViewModel() {
    abstract val extensionsFlow: StateFlow<List<T>>
    abstract val currentSelectionFlow: MutableStateFlow<T?>
    fun selectExtension(index: Int) {
        val extension = extensionsFlow.value.getOrNull(index) ?: return
        currentSelectionFlow.value = extension
        onExtensionSelected(extension)
    }

    open fun onExtensionSelected(extension: T) {}
}