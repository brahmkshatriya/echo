package dev.brahmkshatriya.echo.extensions.plugger.interfaces

import dev.brahmkshatriya.echo.common.helpers.Injectable
import kotlinx.coroutines.flow.Flow

interface PluginRepo<TMetadata, TPlugin> {
    fun getAllPlugins(): Flow<List<Result<Pair<TMetadata, Injectable<TPlugin>>>>>
}