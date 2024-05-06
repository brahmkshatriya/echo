package dev.brahmkshatriya.echo.ui.playlist

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.EditPlayerListenerClient
import dev.brahmkshatriya.echo.common.clients.EditPlaylistCoverClient
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.getClient
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
    val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
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
        val client = extensionListFlow.getClient(clientId)?.client
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

    val closing = MutableStateFlow<Boolean?>(null)
    private val actions = mutableListOf<ListAction<Track>>()
    val currentTracks = MutableStateFlow<List<Track>?>(null)
    fun edit(action: ListAction<Track>) {
        currentTracks.value = currentTracks.value?.toMutableList()?.apply {
            when (action) {
                is ListAction.Add -> addAll(action.index!!, action.items)
                is ListAction.Move -> add(action.to, removeAt(action.from))
                is ListAction.Remove -> action.indexes.forEach { removeAt(it) }
            }
            actions.add(action)
        }
    }

    fun deletePlaylist(clientId: String, playlist: Playlist) = viewModelScope.launch {
        deletePlaylist(extensionListFlow, mutableMessageFlow, context, clientId, playlist)
    }

    suspend fun onEditorEnter(clientId: String, playlist: Playlist) {
        client<EditPlayerListenerClient>(clientId) {
            it.onEnterPlaylistEditor(playlist)
        }
    }

    private fun mergeActions(actions: MutableList<ListAction<Track>>) {
        var i = 0
        while (i < actions.size - 1) {
            val curr = actions[i]
            val next = actions[i + 1]
            if (curr is ListAction.Add && next is ListAction.Add && curr.index!! + curr.items.size == next.index) {
                curr.items.addAll(next.items)
                actions.removeAt(i + 1)
            } else if (curr is ListAction.Move && next is ListAction.Move && curr.to == next.from) {
                actions[i] = ListAction.Move(curr.from, next.to)
                actions.removeAt(i + 1)
            } else if (curr is ListAction.Remove && next is ListAction.Remove) {
                actions[i] = ListAction.Remove(curr.indexes + next.indexes)
                actions.removeAt(i + 1)
            } else {
                i++
            }
        }
    }

    fun onEditorExit(
        clientId: String,
        playlist: Playlist,
        block: suspend (action: ListAction<Track>?) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        mergeActions(actions)
        val newActions = actions.toList().takeIf { it.isNotEmpty() }
            ?: return@launch
        libraryClient(clientId) { client ->
            newActions.forEach {
                println(it)
                when (it) {
                    is ListAction.Add -> client.addTracksToPlaylist(playlist, it.index, it.items)
                    is ListAction.Move -> client.moveTrackInPlaylist(playlist, it.from, it.to)
                    is ListAction.Remove -> client.removeTracksFromPlaylist(playlist, it.indexes)
                }
                withContext(Dispatchers.Main) { block(it) }
            }
        }
        block(null)
        actions.clear()
        client<EditPlayerListenerClient>(clientId) {
            it.onExitPlaylistEditor(playlist)
        }
    }

    sealed class ListAction<T> {
        data class Add<T>(val index: Int?, val items: MutableList<T>) : ListAction<T>()
        data class Remove<T>(val indexes: List<Int>) : ListAction<T>() {
            constructor(vararg indexes: Int) : this(indexes.toList())
        }

        data class Move<T>(val from: Int, val to: Int) : ListAction<T>()
    }

    companion object {
        suspend fun CatchingViewModel.deletePlaylist(
            extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
            mutableMessageFlow: MutableSharedFlow<SnackBar.Message>,
            context: Context,
            clientId: String,
            playlist: Playlist
        ) {
            val client = extensionListFlow.getClient(clientId)?.client ?: return
            if (client !is LibraryClient) return
            withContext(Dispatchers.IO) {
                tryWith { client.deletePlaylist(playlist) } ?: return@withContext
                mutableMessageFlow.emit(SnackBar.Message(context.getString(R.string.playlist_deleted)))
            }
        }

        suspend fun CatchingViewModel.addToPlaylists(
            extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
            messageFlow: MutableSharedFlow<SnackBar.Message>,
            context: Context,
            clientId: String,
            playlists: List<Playlist>,
            tracks: List<Track>
        ) = run {
            val client = extensionListFlow.getClient(clientId)?.client ?: return
            if (client !is LibraryClient) return
            val listener = client as? EditPlayerListenerClient
            withContext(Dispatchers.IO) {
                playlists.forEach { playlist ->
                    tryWith {
                        check(playlist.isEditable)
                        listener?.onEnterPlaylistEditor(playlist)
                        client.addTracksToPlaylist(playlist, null, tracks)
                        listener?.onExitPlaylistEditor(playlist)
                    }
                }
            }
            val message = if (playlists.size == 1)
                context.getString(R.string.saved_to_playlist, playlists.first().title)
            else context.getString(R.string.saved_to_playlists)
            messageFlow.emit(SnackBar.Message(message))
        }
    }
}