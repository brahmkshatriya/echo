package dev.brahmkshatriya.echo.ui.editplaylist

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditCoverClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditorListenerClient
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.extensions.getExtension
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
    private val context: Application,
) : CatchingViewModel(throwableFlow) {

    var loading: Boolean? = null
    val loadingFlow = MutableSharedFlow<Boolean?>()
    fun load(clientId: String?, playlist: Playlist) {
        if (loading == true) return
        loading = true
        viewModelScope.launch {
            loadingFlow.emit(true)
            val extension = extensionListFlow.getExtension(clientId) ?: return@launch
            withContext(Dispatchers.IO) {
                originalList = extension.get<PlaylistClient, List<Track>>(throwableFlow) {
                    loadTracks(playlist).loadAll()
                } ?: emptyList()
                currentTracks.value = originalList
                loading = null
                loadingFlow.emit(null)
            }
        }
    }

    private suspend fun playlistEditClient(
        clientId: String, block: suspend (client: PlaylistEditClient) -> Unit
    ) = client(clientId, block)

    private suspend inline fun <reified T> client(
        clientId: String, crossinline block: suspend (client: T) -> Unit
    ) {
        val extension = extensionListFlow.getExtension(clientId) ?: return
        withContext(Dispatchers.IO) {
            extension.get<T, Unit>(throwableFlow) { block.invoke(this) }
        }
    }

    fun changeMetadata(clientId: String, playlist: Playlist, title: String, description: String?) {
        viewModelScope.launch {
            playlistEditClient(clientId) {
                it.editPlaylistMetadata(playlist, title, description)
            }
        }
    }

    fun changeCover(clientId: String, playlist: Playlist, file: File?) {
        viewModelScope.launch {
            client<PlaylistEditCoverClient>(clientId) {
                it.editPlaylistCover(playlist, file)
            }
        }
    }

    private var originalList: List<Track> = emptyList()
    private val actions = mutableListOf<ListAction<Track>>()
    val performedActions = MutableSharedFlow<Pair<List<Track>, ListAction<Track>?>>()
    val currentTracks = MutableStateFlow<List<Track>?>(null)

    fun edit(action: ListAction<Track>) {
        currentTracks.value = currentTracks.value?.toMutableList()?.apply {
            when (action) {
                is ListAction.Add -> addAll(action.index, action.items)
                is ListAction.Move -> add(action.to, removeAt(action.from))
                is ListAction.Remove -> action.indexes.forEach { removeAt(it) }
            }
            actions.add(action)
        }
    }

    fun deletePlaylist(clientId: String, playlist: Playlist) =
        deletePlaylist(extensionListFlow, mutableMessageFlow, context, clientId, playlist)

    private fun mergeActions(actions: MutableList<ListAction<Track>>) {
        var i = 0
        while (i < actions.size - 1) {
            val curr = actions[i]
            val next = actions[i + 1]
            if (curr is ListAction.Add && next is ListAction.Add && curr.index + curr.items.size == next.index) {
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
        clientId: String, playlist: Playlist
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (loading == true) return@launch
        loading = true
        loadingFlow.emit(true)
//        mergeActions(actions)
        val newActions = actions.toList().takeIf { it.isNotEmpty() }
        if (newActions != null) {
            val tracks = originalList.toMutableList()
            performedActions.emit(tracks to null)
            println("Actions Size : ${newActions.size}")
            client<PlaylistEditorListenerClient>(clientId) {
                it.onEnterPlaylistEditor(playlist, tracks)
            }
            playlistEditClient(clientId) { client ->
                newActions.forEach { action ->
                    println("new action : $action")
                    performedActions.emit(tracks to action)
                    when (action) {
                        is ListAction.Add -> {
                            println("adding action")
                            client.addTracksToPlaylist(playlist, tracks, action.index, action.items)
                            println("added")
                            tracks.addAll(action.index, action.items)
                        }

                        is ListAction.Move -> {
                            client.moveTrackInPlaylist(playlist, tracks, action.from, action.to)
                            tracks.add(action.to, tracks.removeAt(action.from))
                        }

                        is ListAction.Remove -> {
                            client.removeTracksFromPlaylist(playlist, tracks, action.indexes)
                            action.indexes.forEach { tracks.removeAt(it) }
                        }
                    }
                }
            }
            performedActions.emit(tracks to null)
            client<PlaylistEditorListenerClient>(clientId) {
                it.onExitPlaylistEditor(playlist, tracks)
            }
        }
        loading = false
        loadingFlow.emit(false)
        currentTracks.value = null
        actions.clear()
        originalList = emptyList()
        loading = null
    }


    sealed class ListAction<T> {
        data class Add<T>(val index: Int, val items: MutableList<T>) : ListAction<T>()
        data class Remove<T>(val indexes: List<Int>) : ListAction<T>() {
            constructor(vararg indexes: Int) : this(indexes.toList())
        }

        data class Move<T>(val from: Int, val to: Int) : ListAction<T>()
    }

    companion object {
        fun CatchingViewModel.deletePlaylist(
            extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
            mutableMessageFlow: MutableSharedFlow<SnackBar.Message>,
            context: Context,
            clientId: String,
            playlist: Playlist
        ) {
            val extension = extensionListFlow.getExtension(clientId) ?: return
            viewModelScope.launch(Dispatchers.IO) {
                extension.get<PlaylistEditClient, Unit>(throwableFlow) {
                    println("deleting playlist : ${playlist.title}")
                    deletePlaylist(playlist)
                    println("deleted playlist : ${playlist.title}")
                }
                mutableMessageFlow.emit(SnackBar.Message(context.getString(R.string.playlist_deleted)))
            }
        }

        suspend fun CatchingViewModel.addToPlaylists(
            extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
            messageFlow: MutableSharedFlow<SnackBar.Message>,
            context: Context,
            clientId: String,
            playlists: List<Playlist>,
            new: List<Track>
        ) = run {
            val extension = extensionListFlow.getExtension(clientId) ?: return
            withContext(Dispatchers.IO) {
                playlists.forEach { playlist ->
                    extension.get<PlaylistEditClient, Unit>(throwableFlow) {
                        val loaded = loadPlaylist(playlist)
                        check(loaded.isEditable)
                        val tracks = loadTracks(loaded).loadAll()
                        val listener = this as? PlaylistEditorListenerClient
                        listener?.onEnterPlaylistEditor(loaded, tracks)
                        addTracksToPlaylist(loaded, tracks, tracks.size, new)
                        listener?.onExitPlaylistEditor(loaded, tracks)
                        Unit
                    } ?: return@withContext
                }
                val message =
                    if (playlists.size != 1) context.getString(R.string.saved_to_playlists)
                    else context.getString(R.string.saved_to_playlist, playlists.first().title)
                messageFlow.emit(SnackBar.Message(message))
            }
        }
    }
}