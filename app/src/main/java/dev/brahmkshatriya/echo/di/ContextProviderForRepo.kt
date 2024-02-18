package dev.brahmkshatriya.echo.di

import android.content.Context
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tel.jeelpa.plugger.PluginRepo

class ContextProviderForRepo(
    private val context: Context,
    private val pluginRepo: PluginRepo<ExtensionClient>
) : PluginRepo<ExtensionClient> {
    override fun getAllPlugins(): Flow<List<ExtensionClient>> =
        pluginRepo.getAllPlugins().map { list ->
            list.onEach { it.context = context }
        }
}