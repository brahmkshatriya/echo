package dev.brahmkshatriya.echo.ui.extensions.add

import androidx.lifecycle.ViewModel
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.Updater
import kotlinx.coroutines.flow.MutableStateFlow

class AddViewModel(
    val extensionLoader: ExtensionLoader,
    val list: List<Updater.ExtensionAssetResponse>,
) : ViewModel() {
    private val installed = extensionLoader.extensions.all.value.orEmpty().map { it.id }
    val listFlow = MutableStateFlow(list.map {
        val isInstalled = it.id in installed
        ExtensionsAddListAdapter.Item(it, !isInstalled, isInstalled)
    })

    fun selectAll(select: Boolean) {
        val list = listFlow.value
        listFlow.value = list.map { it.copy(isChecked = select) }
    }

    fun toggleItem(item: Updater.ExtensionAssetResponse, isChecked: Boolean) {
        val list = listFlow.value
        val index = list.indexOfFirst { it.item.id == item.id }
        if (index == -1) return
        val currentItem = list[index]
        listFlow.value = list.toMutableList().apply {
            set(index, currentItem.copy(isChecked = isChecked))
        }
    }
}