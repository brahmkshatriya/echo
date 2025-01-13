package dev.brahmkshatriya.echo.extensions

import android.content.Context
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.Injectable
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.utils.getSettings
import tel.jeelpa.plugger.utils.combineStates
import tel.jeelpa.plugger.utils.mapState

class InjectableRepoComposer<TPlugin : ExtensionClient>(
    private val context: Context,
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
        getOrNull()?.setSettings(getSettings(context, type, metadata))
    }
}