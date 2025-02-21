package dev.brahmkshatriya.echo.extensions.plugger.impl

import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.helpers.Injectable
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.extensions.plugger.interfaces.PluginRepo
import kotlinx.coroutines.flow.MutableStateFlow

class BuiltInRepo(
    private val list: List<Pair<Metadata, Injectable<ExtensionClient>>>
) : PluginRepo<Metadata, ExtensionClient> {
    override fun getAllPlugins() = MutableStateFlow(list.map { Result.success(it) })
}