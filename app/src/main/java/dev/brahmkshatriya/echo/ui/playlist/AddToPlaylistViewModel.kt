package dev.brahmkshatriya.echo.ui.playlist

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.EditPlayerListenerClient
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionListFlow: ExtensionModule.ExtensionListFlow,
    private val messageFlow: MutableSharedFlow<SnackBar.Message>,
    private val app: Application,
) : CatchingViewModel(throwableFlow) {
    val playlists = MutableStateFlow<List<Playlist>?>(null)
    val selectedPlaylists = mutableListOf<Playlist>()
    lateinit var clientId: String

    override fun onInitialize() {
        val client = extensionListFlow.getClient(clientId) ?: return
        if (client !is LibraryClient) return
        viewModelScope.launch(Dispatchers.IO) {
            playlists.value = tryWith { client.listEditablePlaylists() }
            if (playlists.value == null) dismiss.emit(Unit)
        }
    }

    fun togglePlaylist(playlist: Playlist) = with(selectedPlaylists) {
        if (contains(playlist)) remove(playlist) else add(playlist)
    }

    var saving = false
    val dismiss = MutableSharedFlow<Unit>()
    fun addToPlaylists(list: List<Track>) = viewModelScope.launch(Dispatchers.IO) {
        val client = extensionListFlow.getClient(clientId) ?: return@launch
        if (client !is LibraryClient) return@launch
        val listener = client as? EditPlayerListenerClient
        saving = true
        selectedPlaylists.forEach { playlist ->
            tryWith {
                check(playlist.isEditable)
                listener?.onEnterPlaylistEditor(playlist)
                client.addTracksToPlaylist(playlist, null, list)
                listener?.onExitPlaylistEditor(playlist)
            }
        }
        saving = false
        dismiss.emit(Unit)
        val message = if (selectedPlaylists.size == 1)
            app.getString(R.string.saved_to_playlist, selectedPlaylists.first().title)
        else app.getString(R.string.saved_to_playlists)
        messageFlow.emit(SnackBar.Message(message))
    }
}
