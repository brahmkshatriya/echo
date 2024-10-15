package dev.brahmkshatriya.echo.extensions.plugger

import tel.jeelpa.plugger.ManifestParser
import tel.jeelpa.plugger.PluginLoader
import tel.jeelpa.plugger.PluginSource
import tel.jeelpa.plugger.utils.mapState

data class LazyPluginRepoImpl<TSourceInput, TMetadata, TPlugin : Any>(
    private val pluginSource: PluginSource<TSourceInput>,
    private val manifestParser: ManifestParser<TSourceInput, TMetadata>,
    private val pluginLoader: PluginLoader<TMetadata, TPlugin>
) : LazyPluginRepo<TMetadata, TPlugin> {

    override fun getAllPlugins() = pluginSource.getSourceFiles().mapState { files ->
        files.map {
            runCatching { manifestParser.parseManifest(it) }
        }
    }.mapState { metadata ->
        metadata.map { resultMetadata ->
            resultMetadata.mapCatching {
                 it to lazily { pluginLoader.loadPlugin(it) }
            }
        }
    }
}