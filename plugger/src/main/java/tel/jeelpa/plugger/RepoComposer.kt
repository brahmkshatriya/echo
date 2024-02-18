package tel.jeelpa.plugger

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RepoComposer<TPlugin>(private vararg val repos: PluginRepo<TPlugin>) : PluginRepo<TPlugin> {
    override fun getAllPlugins(): Flow<List<TPlugin>> =
        combine(
            repos.map { it.getAllPlugins() }
        ) { array ->
            array.reduce { a, b -> a + b }
        }
}
