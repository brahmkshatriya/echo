package dev.brahmkshatriya.echo.plugger

import android.content.Context
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import kotlinx.coroutines.flow.map
import tel.jeelpa.plugger.PluginRepo

class RepoWithPreferences(
    private val context: Context,
    private val pluginRepo: PluginRepo<ExtensionClient>
) : PluginRepo<ExtensionClient> {
    override fun getAllPlugins(exceptionHandler: (Exception) -> Unit) =
        pluginRepo.getAllPlugins(exceptionHandler).map { list ->
            list.onEach {
                it.setPreferences(
                    context.getSharedPreferences(
                        it.metadata.id,
                        Context.MODE_PRIVATE
                    )
                )
            }
        }
}
