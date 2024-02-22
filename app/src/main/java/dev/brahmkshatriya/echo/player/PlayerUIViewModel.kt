package dev.brahmkshatriya.echo.player

import androidx.lifecycle.ViewModel
import dev.brahmkshatriya.echo.common.models.Track
import kotlinx.coroutines.flow.MutableStateFlow

class PlayerUIViewModel : ViewModel() {
    var playlistTranslationY: Int = 0
    var bottomNavTranslateY: Int = 0
    var repeatMode: Int = 0

    val track = MutableStateFlow<Track?>(null)
    val playlist = MutableStateFlow(0)

    val progress = MutableStateFlow(0 to 0)
    val totalDuration = MutableStateFlow(0)

    val buffering = MutableStateFlow(false)
    val isPlaying = MutableStateFlow(false)
    val nextEnabled = MutableStateFlow(false)
    val previousEnabled = MutableStateFlow(false)
}