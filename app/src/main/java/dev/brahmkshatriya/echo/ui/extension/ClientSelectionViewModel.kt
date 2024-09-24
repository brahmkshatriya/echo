package dev.brahmkshatriya.echo.ui.extension

import dev.brahmkshatriya.echo.plugger.echo.ExtensionMetadata
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class ClientSelectionViewModel(
    throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {

    abstract val metadataFlow: StateFlow<List<ExtensionMetadata>?>
    abstract val currentFlow : StateFlow<String?>
    abstract fun onClientSelected(clientId: String)
}