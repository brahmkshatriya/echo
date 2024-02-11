package dev.brahmkshatriya.echo.ui.player

import androidx.lifecycle.ViewModel
import androidx.media3.session.MediaController
import dev.brahmkshatriya.echo.data.models.Track
import kotlinx.coroutines.flow.MutableStateFlow

class PlayerUIViewModel : ViewModel() {

    val track = MutableStateFlow<Track?>(null)

    val progress = MutableStateFlow(0 to 0)
    val totalDuration: MutableStateFlow<Int> = MutableStateFlow(0)

    val buffering = MutableStateFlow(false)
    val isPlaying = MutableStateFlow(false)
    val nextEnabled = MutableStateFlow(false)
    val previousEnabled = MutableStateFlow(false)

    private var listener: PlayerListener? = null
    fun getListener(player: MediaController) =
        listener ?: PlayerListener(player, this).also { listener = it }
}