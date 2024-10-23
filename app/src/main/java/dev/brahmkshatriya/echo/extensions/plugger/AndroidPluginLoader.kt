package dev.brahmkshatriya.echo.extensions.plugger

import android.content.Context
import dev.brahmkshatriya.echo.common.models.Metadata
import tel.jeelpa.plugger.PluginLoader
import tel.jeelpa.plugger.pluginloader.GetClassLoaderWithPathUseCase

class AndroidPluginLoader<TPlugin>(
    private val getClassLoader: GetClassLoaderWithPathUseCase
) : PluginLoader<Metadata, TPlugin> {
    constructor(context: Context): this(GetClassLoaderWithPathUseCase(context.classLoader))

    @Suppress("UNCHECKED_CAST")
    override fun loadPlugin(pluginMetadata: Metadata): TPlugin {
        return getClassLoader.getWithPath(pluginMetadata.path)
            .loadClass(pluginMetadata.className)
            .getConstructor()
            .newInstance() as TPlugin
    }
}