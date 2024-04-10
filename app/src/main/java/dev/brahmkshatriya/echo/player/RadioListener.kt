package dev.brahmkshatriya.echo.player

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.AUTO_START_RADIO
import dev.brahmkshatriya.echo.utils.tryWith
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.radioNotSupported
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class RadioListener(
    private val context: Context,
    private val player: Player,
    private val extensionListFlow: ExtensionModule.ExtensionListFlow,
    private val queue: Queue,
    private val scope: CoroutineScope,
    private val settings: SharedPreferences,
    private val throwableFlow: MutableSharedFlow<Throwable>,
    private val messageFlow: MutableSharedFlow<SnackBar.Message>
) : Player.Listener {

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (!player.hasNextMediaItem()) {
            val autoStartRadio = settings.getBoolean(AUTO_START_RADIO, true)
            if (autoStartRadio) queue.current?.let {
                scope.launch(Dispatchers.IO) {
                    val client = extensionListFlow.getClient(it.clientId)
                    radio(context, client, messageFlow, queue) {
                        tryWith(throwableFlow) { radio(it.unloaded) }
                    }
                }
            }
        }
    }

    companion object {
        suspend fun radio(
            context: Context,
            client: ExtensionClient?,
            messageFlow: MutableSharedFlow<SnackBar.Message>,
            queue: Queue,
            block: suspend RadioClient.() -> Playlist?
        ) = when (client) {
            null -> {
                messageFlow.emit(context.noClient())
                null
            }

            is RadioClient -> {
                val tracks = block(client)?.tracks
                tracks?.let { queue.addTracks(client.metadata.id, it).first }
            }

            else -> {
                messageFlow.emit(context.radioNotSupported(client.metadata.name))
                null
            }
        }
    }
}