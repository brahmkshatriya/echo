package dev.brahmkshatriya.echo.plugger

import android.content.Context
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import kotlinx.coroutines.flow.StateFlow
import tel.jeelpa.plugger.PluginRepo

data class LyricsExtension(
    val metadata: ExtensionMetadata,
    val client: LyricsClient,
)

fun StateFlow<List<LyricsExtension>?>.getClient(id: String?) =
    value?.find { it.metadata.id == id }

class LyricsExtensionRepo(
    private val context: Context,
    private val pluginRepo: PluginRepo<ExtensionMetadata, LyricsClient>
) : PluginRepo<ExtensionMetadata, LyricsClient> {
    override fun getAllPlugins() = context.injectSettings<LyricsClient>(pluginRepo)
}