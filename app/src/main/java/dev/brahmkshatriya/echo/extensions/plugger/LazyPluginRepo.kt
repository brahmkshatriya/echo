package dev.brahmkshatriya.echo.extensions.plugger

import tel.jeelpa.plugger.PluginRepo
import tel.jeelpa.plugger.utils.combineStates

interface LazyPluginRepo<T, R> : PluginRepo<T, Lazy<Result<R>>>

class LazyRepoComposer<TMetadata, TPlugin>(
    private vararg val repos: LazyPluginRepo<TMetadata, TPlugin>
) : LazyPluginRepo<TMetadata, TPlugin> {
    override fun getAllPlugins() = repos.map { it.getAllPlugins() }
        .reduce { a, b -> combineStates(a, b) { x, y -> x + y } }
}

fun <T> lazily(value: T) = lazy { runCatching { value } }