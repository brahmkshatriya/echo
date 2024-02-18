package dev.brahmkshatriya.echo.data.extensions

import dev.brahmkshatriya.echo.common.data.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.data.extensions.OfflineExtension
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import tel.jeelpa.plugger.PluginRepo

class LocalExtensionRepo : PluginRepo<ExtensionClient> {
    override fun getAllPlugins(): Flow<List<ExtensionClient>> =
        flowOf(listOf(OfflineExtension()))
}