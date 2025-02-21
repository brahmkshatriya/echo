package dev.brahmkshatriya.echo.extensions.plugger.interfaces

interface PluginLoader<TMetadata, TPlugin> {
    fun loadPlugin(pluginMetadata: TMetadata): TPlugin
}