package dev.brahmkshatriya.echo.player

import android.content.SharedPreferences
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.ui.settings.AudioFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RadioListener(
    private val player: Player,
    private val queue: Queue,
    private val scope: CoroutineScope,
    private val settings: SharedPreferences,
    extensionFlow: Flow<ExtensionClient?>,
) : Player.Listener {

    private var client: RadioClient? = null

    init {
        scope.launch {
            extensionFlow.collectLatest {
                client = it as? RadioClient
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (!player.hasNextMediaItem()) {
            val autoStartRadio = settings.getBoolean(AudioFragment.AUTO_START_RADIO, true)
            if(autoStartRadio) queue.current?.let {
                scope.launch(Dispatchers.IO) {
                    val playlist = client?.radio(it)
                    playlist?.tracks?.let { queue.addTracks(scope, it) }
                }
            }
        }
    }
}