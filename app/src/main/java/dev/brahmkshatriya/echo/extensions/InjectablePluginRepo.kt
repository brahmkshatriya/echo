package dev.brahmkshatriya.echo.extensions

import dev.brahmkshatriya.echo.common.helpers.Injectable
import tel.jeelpa.plugger.ManifestParser
import tel.jeelpa.plugger.PluginLoader
import tel.jeelpa.plugger.PluginRepo
import tel.jeelpa.plugger.PluginSource
import tel.jeelpa.plugger.utils.mapState

interface InjectablePluginRepo<T, R> : PluginRepo<T, Injectable<R>>

data class InjectablePluginRepoImpl<TSourceInput, TMetadata, TPlugin : Any>(
    private val pluginSource: PluginSource<TSourceInput>,
    private val manifestParser: ManifestParser<TSourceInput, TMetadata>,
    private val pluginLoader: PluginLoader<TMetadata, TPlugin>
) : InjectablePluginRepo<TMetadata, TPlugin> {

    override fun getAllPlugins() = pluginSource.getSourceFiles().mapState { files ->
        files.map {
            runCatching { manifestParser.parseManifest(it) }
        }
    }.mapState { metadata ->
        metadata.map { resultMetadata ->
            resultMetadata.mapCatching {
                 it to Injectable { pluginLoader.loadPlugin(it) }
            }
        }
    }
}