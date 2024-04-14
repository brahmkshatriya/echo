package dev.brahmkshatriya.echo.plugger

import android.content.Context
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.offline.OfflineExtension
import kotlinx.coroutines.flow.flowOf
import tel.jeelpa.plugger.PluginRepo

class LocalExtensionRepo(val context: Context) : PluginRepo<ExtensionClient> {
    override fun getAllPlugins(exceptionHandler: (Exception) -> Unit) =
        flowOf(
            listOf(
                OfflineExtension(context),
            )
        )
}