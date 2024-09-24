package dev.brahmkshatriya.echo.plugger.echo

import android.content.Context
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.models.ExtensionType
import kotlinx.coroutines.flow.StateFlow
import tel.jeelpa.plugger.PluginRepo

data class MusicExtension(
    override val metadata: ExtensionMetadata,
    override val client: ExtensionClient,
) : GenericExtension(ExtensionType.MUSIC, metadata, client)

fun StateFlow<List<MusicExtension>?>.getExtension(id: String?) =
    value?.find { it.metadata.id == id }

class MusicExtensionRepo(
    private val context: Context,
    private val pluginRepo: PluginRepo<ExtensionMetadata, ExtensionClient>
) : PluginRepo<ExtensionMetadata, ExtensionClient> {

    override fun getAllPlugins() = pluginRepo.getAllPlugins()
        .injectSettings(ExtensionType.MUSIC, context)
//        .injectContext(context)
}

