package dev.brahmkshatriya.echo.player

import android.content.SharedPreferences
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.ui.settings.AudioFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RadioListener(
    private val player: Player,
    private val queue: Queue,
    private val scope: CoroutineScope,
    private val settings: SharedPreferences
) : Player.Listener {

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (!player.hasNextMediaItem()) {
            val autoStartRadio = settings.getBoolean(AudioFragment.AUTO_START_RADIO, true)
            if (autoStartRadio) queue.current?.let {
                scope.launch(Dispatchers.IO) {
                    val radioClient = queue.current?.client as? RadioClient ?: return@launch
                    val playlist = radioClient.radio(it.track)
                    val client = radioClient as? TrackClient ?: return@launch
                    queue.addTracks(client, playlist.tracks)
                }
            }
        }
    }
}