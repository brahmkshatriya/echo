package dev.brahmkshatriya.echo.extensions

import android.content.Context
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.Injectable
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.providers.MessageFlowProvider
import dev.brahmkshatriya.echo.common.providers.MetadataProvider
import dev.brahmkshatriya.echo.utils.getSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import tel.jeelpa.plugger.utils.combineStates
import tel.jeelpa.plugger.utils.mapState

class InjectableRepoComposer<TPlugin : ExtensionClient>(
    private val context: Context,
    private val messageFlow: MutableSharedFlow<Message>,
    private val type: ExtensionType,
    private vararg val repos: InjectablePluginRepo<Metadata, TPlugin>
) : InjectablePluginRepo<Metadata, TPlugin> {
    override fun getAllPlugins() = repos.map { it.getAllPlugins() }
        .reduce { a, b -> combineStates(a, b) { x, y -> x + y } }
        .mapState { list ->
            list.forEach { it.getOrNull()?.run { second.injected(first) } }
            list.groupBy { it.getOrNull()?.first?.id }.map { entry ->
                entry.value.minBy {
                    it.getOrNull()?.first?.importType?.ordinal ?: Int.MAX_VALUE
                }
            }
        }

    private fun Injectable<TPlugin>.injected(metadata: Metadata) = inject {
        val instance = getOrNull() ?: return@inject
        if (instance is MetadataProvider) instance.setMetadata(metadata)
        if (instance is MessageFlowProvider) instance.setMessageFlow(messageFlow)
        instance.setSettings(getSettings(context, type, metadata))
    }
}