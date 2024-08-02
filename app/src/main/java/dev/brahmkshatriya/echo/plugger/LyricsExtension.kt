package dev.brahmkshatriya.echo.plugger

import android.content.Context
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.plugger.ExtensionInfo.Companion.toExtensionInfo
import kotlinx.coroutines.flow.StateFlow
import tel.jeelpa.plugger.PluginRepo

data class LyricsExtension(
    override val metadata: ExtensionMetadata,
    override val client: LyricsClient,
    override val info: ExtensionInfo = metadata.toExtensionInfo(ExtensionType.LYRICS)
) : GenericExtension(ExtensionType.LYRICS, metadata, client, info)

fun StateFlow<List<LyricsExtension>?>.getExtension(id: String?) =
    value?.find { it.metadata.id == id }

class LyricsExtensionRepo(
    private val context: Context,
    private val pluginRepo: PluginRepo<ExtensionMetadata, LyricsClient>
) : PluginRepo<ExtensionMetadata, LyricsClient> {
    override fun getAllPlugins() = context
        .injectSettings<LyricsClient>(ExtensionType.LYRICS, pluginRepo)
}