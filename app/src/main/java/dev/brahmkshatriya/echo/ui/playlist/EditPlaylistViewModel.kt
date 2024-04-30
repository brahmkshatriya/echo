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
import dev.brahmkshatriya.echo.utils.ListAction
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EditPlaylistViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionListFlow: ExtensionModule.ExtensionListFlow,
    private val mutableMessageFlow: MutableSharedFlow<SnackBar.Message>,
    private val context: Application
) : CatchingViewModel(throwableFlow) {

    private suspend fun libraryClient(
        clientId: String,
        block: suspend (client: LibraryClient) -> Unit
    ) {
        client(clientId, block)
    }

    private suspend inline fun <reified T> client(
        clientId: String, crossinline block: suspend (client: T) -> Unit
    ) {
        val client = extensionListFlow.getClient(clientId)
        if (client !is T) return
        withContext(Dispatchers.IO) {
            tryWith { block.invoke(client) }
        }
    }

    fun changeMetadata(clientId: String, playlist: Playlist, title: String, description: String?) {
        viewModelScope.launch {
            libraryClient(clientId) {
                it.editPlaylistMetadata(playlist, title, description)
            }
        }
    }

    fun changeCover(clientId: String, playlist: Playlist, file: File?) {
        viewModelScope.launch {
            client<EditPlaylistCoverClient>(clientId) {
                it.editPlaylistCover(playlist, file)
            }
        }
    }

    val currentTracks = MutableStateFlow<List<Track>?>(null)
    fun edit(block: MutableList<Track>.() -> Unit) {
        currentTracks.value = currentTracks.value?.toMutableList()?.apply {
            block()
        }
    }

    fun deletePlaylist(clientId: String, playlist: Playlist) {
        deletePlaylist(extensionListFlow, mutableMessageFlow, context, clientId, playlist)
    }

    suspend fun onEditorEnter(clientId: String, playlist: Playlist) {
        client<EditPlayerListenerClient>(clientId) {
            it.onEnterPlaylistEditor(playlist)
        }
    }

    suspend fun onEditorExit(clientId: String, playlist: Playlist) {
        val actions = currentTracks.value?.let { tracks ->
            ListAction.getActions(playlist.tracks, tracks) { it.id == this.id }
        }?.takeIf { it.isNotEmpty() } ?: return

        libraryClient(clientId) { client ->
            actions.forEach {
                when (it) {
                    is ListAction.Add -> client.addTracksToPlaylist(playlist, it.index, it.items)
                    is ListAction.Move -> client.moveTrackInPlaylist(playlist, it.from, it.to)
                    is ListAction.Remove -> client.removeTracksFromPlaylist(playlist, it.items)
                }
            }
        }

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