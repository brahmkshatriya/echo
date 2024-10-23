package dev.brahmkshatriya.echo.extensions.plugger

import dev.brahmkshatriya.echo.common.models.Metadata
import tel.jeelpa.plugger.PluginRepo
import tel.jeelpa.plugger.utils.combineStates
import tel.jeelpa.plugger.utils.mapState

interface LazyPluginRepo<T, R> : PluginRepo<T, Lazy<Result<R>>>

class LazyRepoComposer<TPlugin>(
    private vararg val repos: LazyPluginRepo<Metadata, TPlugin>
) : LazyPluginRepo<Metadata, TPlugin> {
    override fun getAllPlugins() = repos.map { it.getAllPlugins() }
        .reduce { a, b -> combineStates(a, b) { x, y -> x + y } }
        .mapState { list ->
            list.groupBy { it.getOrNull()?.first?.id }.map { entry ->
                entry.value.minBy {
                    it.getOrNull()?.first?.importType?.ordinal ?: Int.MAX_VALUE
                }
            }
        }
}

fun <T> catchLazy(function: () -> T) = lazy { runCatching { function() } }