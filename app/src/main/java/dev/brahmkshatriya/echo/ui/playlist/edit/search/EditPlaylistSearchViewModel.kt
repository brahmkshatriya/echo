package dev.brahmkshatriya.echo.ui.playlist.edit.search

import androidx.lifecycle.ViewModel
import dev.brahmkshatriya.echo.common.models.Track
import kotlinx.coroutines.flow.MutableStateFlow

class EditPlaylistSearchViewModel : ViewModel() {

    val selectedTracks = MutableStateFlow<List<Track>>(listOf())
    fun toggleTrack(track: Track) = with(selectedTracks) {
        val mutable = selectedTracks.value.toMutableList()
        with(mutable) { if (contains(track)) remove(track) else add(track) }
        value = mutable
    }

    fun addTrack(track: Track) = with(selectedTracks) {
        if(value.contains(track)) return@with
        val mutable = selectedTracks.value.toMutableList()
        mutable.add(track)
        value = mutable
    }
}