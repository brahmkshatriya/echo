package dev.brahmkshatriya.echo.extensions.plugger.impl

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.helpers.Injectable
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.extensions.exceptions.ExtensionLoaderException
import dev.brahmkshatriya.echo.extensions.plugger.interfaces.ManifestParser
import dev.brahmkshatriya.echo.extensions.plugger.interfaces.PluginLoader
import dev.brahmkshatriya.echo.extensions.plugger.interfaces.PluginRepo
import dev.brahmkshatriya.echo.extensions.plugger.interfaces.PluginSource
import kotlinx.coroutines.flow.map

class InjectablePluginRepo<T : Any>(
    private val pluginSource: PluginSource<T>,
    private val manifestParser: ManifestParser<T, Metadata>,
    private val pluginLoader: PluginLoader<Metadata, ExtensionClient>
) : PluginRepo<Metadata, ExtensionClient> {
    override fun getAllPlugins() = pluginSource.getSourceFiles().map { list ->
        list.map { source ->
            runCatching {
                runCatching {
                    val metadata = manifestParser.parseManifest(source)
                    val injectable = Injectable { pluginLoader.loadPlugin(metadata) }
                    metadata to injectable
                }.getOrElse {
                    throw ExtensionLoaderException(javaClass.simpleName, source.toString(), it)
                }
            }
        }
    }
}