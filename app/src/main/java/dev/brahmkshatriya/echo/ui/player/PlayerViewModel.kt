package dev.brahmkshatriya.echo.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.data.clients.TrackClient
import dev.brahmkshatriya.echo.data.models.StreamableAudio
import dev.brahmkshatriya.echo.data.models.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val trackClient: TrackClient
) : ViewModel() {

    val audioFlow = MutableStateFlow<Pair<Track, StreamableAudio>?>(null)

    private fun loadStreamable(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            audioFlow.value = track to trackClient.getStreamable(track)
        }
    }

    fun play(track: Track) {
        loadStreamable(track)
    }

}