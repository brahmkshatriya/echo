package dev.brahmkshatriya.echo.ui.playlist

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.EditPlayerListenerClient
import dev.brahmkshatriya.echo.common.clients.EditPlaylistCoverClient
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
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EditPlaylistViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionListFlow: ExtensionModule.ExtensionListFlow,
    private val mutableMessageFlow: MutableSharedFlow<SnackBar.Message>,
    private val context: Application
) : CatchingViewModel(throwableFlow) {

    private fun libraryClient(clientId: String, block: suspend (client: LibraryClient) -> Unit) {
        client(clientId, block)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> client(clientId: String, block: suspend (client: T) -> Unit) {
        val client = extensionListFlow.getClient(clientId) as? T ?: return
        viewModelScope.launch(Dispatchers.IO) {
            tryWith { block.invoke(client) }
        }
    }

    fun changeMetadata(clientId: String, playlist: Playlist, title: String, description: String?) {
        libraryClient(clientId) {
            it.editPlaylistMetadata(playlist, title, description)
        }
    }

    fun changeCover(clientId: String, playlist: Playlist, file: File?) {
        client<EditPlaylistCoverClient>(clientId) {
            it.editPlaylistCover(playlist, file)
        }
    }


    val currentTracks = MutableStateFlow<List<Track>?>(null)
    private fun edit(block: MutableList<Track>.() -> Unit) {
        currentTracks.value = currentTracks.value?.toMutableList()?.apply {
            block()
        }
    }

    fun removeTracks(clientId: String, playlist: Playlist, indexes: List<Int>) {
        libraryClient(clientId) { client ->
            client.removeTracksFromPlaylist(playlist, indexes)
            edit { indexes.forEach { removeAt(it) } }
        }
    }

    fun moveTracks(clientId: String, playlist: Playlist, from: Int, to: Int) {
        libraryClient(clientId) { client ->
            client.moveTrackInPlaylist(playlist, from, to)
            edit { add(to, removeAt(from)) }
        }
    }

    fun addTracks(clientId: String, playlist: Playlist, tracks: List<Track>) {
        libraryClient(clientId) { client ->
            client.addTracksToPlaylist(playlist, tracks)
            edit { addAll(tracks) }
        }
    }

    fun deletePlaylist(clientId: String, playlist: Playlist) {
        deletePlaylist(extensionListFlow, mutableMessageFlow, context, clientId, playlist)
    }

    fun onEditorEnter(clientId: String, playlist: Playlist) {
        client<EditPlayerListenerClient>(clientId) {
            it.onEnterPlaylistEditor(playlist)
        }
    }

    fun onEditorExit(clientId: String, playlist: Playlist) {
        client<EditPlayerListenerClient>(clientId) {
            it.onExitPlaylistEditor(playlist)
        }
    }

    companion object {
        fun CatchingViewModel.deletePlaylist(
            extensionListFlow: ExtensionModule.ExtensionListFlow,
            mutableMessageFlow: MutableSharedFlow<SnackBar.Message>,
            context: Application,
            clientId: String,
            playlist: Playlist
        ) {
            val client = extensionListFlow.getClient(clientId) as? LibraryClient ?: return
            viewModelScope.launch(Dispatchers.IO) {
                tryWith { client.deletePlaylist(playlist) } ?: return@launch
                mutableMessageFlow.emit(SnackBar.Message(context.getString(R.string.playlist_deleted)))
            }
        }

    }
}