package dev.brahmkshatriya.echo.extensions.plugger.impl

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.helpers.Injectable
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.extensions.plugger.interfaces.ManifestParser
import dev.brahmkshatriya.echo.extensions.plugger.interfaces.PluginLoader
import dev.brahmkshatriya.echo.extensions.plugger.interfaces.PluginRepo
import dev.brahmkshatriya.echo.extensions.plugger.interfaces.PluginSource
import kotlinx.coroutines.flow.map

class InjectablePluginRepo<T>(
    private val pluginSource: PluginSource<T>,
    private val manifestParser: ManifestParser<T, Metadata>,
    private val pluginLoader: PluginLoader<Metadata, ExtensionClient>
) : PluginRepo<Metadata, ExtensionClient> {
    override fun getAllPlugins() = pluginSource.getSourceFiles().map {
        it.map {
            runCatching {
                val metadata = manifestParser.parseManifest(it)
                val injectable = Injectable { pluginLoader.loadPlugin(metadata) }
                metadata to injectable
            }
        }
    }
}