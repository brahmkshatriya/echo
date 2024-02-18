package tel.jeelpa.plugger

import kotlinx.coroutines.flow.Flow

interface PluginRepo<TPlugin> {
    fun getAllPlugins(): Flow<List<TPlugin>>
}