package dev.brahmkshatriya.echo.ui.player.lyrics

import dev.brahmkshatriya.echo.common.LyricsExtension
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.ui.extensions.list.ExtensionListViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class LyricsViewModel(
    extensionLoader: ExtensionLoader
) : ExtensionListViewModel<LyricsExtension>() {
    override val extensionsFlow = extensionLoader.extensions.lyrics
    override val currentSelectionFlow = MutableStateFlow<LyricsExtension?>(null)
}