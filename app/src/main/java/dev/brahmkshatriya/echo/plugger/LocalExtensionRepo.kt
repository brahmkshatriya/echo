package dev.brahmkshatriya.echo.plugger

import android.content.Context
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.offline.OfflineExtension
import dev.brahmkshatriya.echo.offline.TestExtension
import kotlinx.coroutines.flow.MutableStateFlow
import tel.jeelpa.plugger.PluginRepo

class LocalExtensionRepo(val context: Context) : PluginRepo<ExtensionMetadata, ExtensionClient> {
    private val extension = OfflineExtension(context)
    private val test : ExtensionClient = TestExtension()
    override fun getAllPlugins() = MutableStateFlow(
        listOf(
            Result.success(TestExtension.metadata to test),
            Result.success(OfflineExtension.metadata to extension),
        )
    )
}