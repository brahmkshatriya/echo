package dev.brahmkshatriya.echo.plugger

import android.content.Context
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import kotlinx.coroutines.flow.StateFlow
import tel.jeelpa.plugger.PluginRepo

data class MusicExtension(
    val metadata: ExtensionMetadata,
    val client: ExtensionClient,
)

fun StateFlow<List<MusicExtension>?>.getExtension(id: String?) =
    value?.find { it.metadata.id == id }

class MusicExtensionRepo(
    private val context: Context,
    private val pluginRepo: PluginRepo<ExtensionMetadata, ExtensionClient>
) : PluginRepo<ExtensionMetadata, ExtensionClient> {

    override fun getAllPlugins() = context.injectSettings<ExtensionClient>(pluginRepo)
}

