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
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extensions.get
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.ui.editplaylist.EditPlaylistViewModel.Action.Add
import dev.brahmkshatriya.echo.ui.editplaylist.EditPlaylistViewModel.Action.Move
import dev.brahmkshatriya.echo.ui.editplaylist.EditPlaylistViewModel.Action.Remove
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
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
    private val mutableMessageFlow: MutableSharedFlow<Message>,
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
    val performedActions = MutableSharedFlow<Pair<List<Track>, Action<Track>?>>()
    val currentTracks = MutableStateFlow<List<Track>?>(null)

    fun edit(action: Action<Track>) {
        currentTracks.value = currentTracks.value?.toMutableList()?.apply {
            runCatching {
                when (action) {
                    is Add -> addAll(action.index, action.items)
                    is Move -> add(action.to, removeAt(action.from))
                    is Remove -> action.indexes.forEach { removeAt(it) }
                }
            }.getOrElse {
                viewModelScope.launch { throwableFlow.emit(it) }
            }
        }
    }

    fun deletePlaylist(clientId: String, playlist: Playlist) =
        deletePlaylist(extensionListFlow, mutableMessageFlow, context, clientId, playlist)

    fun onEditorExit(
        clientId: String, playlist: Playlist
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (loading == true) return@launch
        loading = true
        loadingFlow.emit(true)
        val newActions = computeActions(originalList, currentTracks.value!!)
        if (newActions.isNotEmpty()) {
            val tracks = originalList.toMutableList()
            performedActions.emit(tracks to null)
            client<PlaylistEditorListenerClient>(clientId) {
                it.onEnterPlaylistEditor(playlist, tracks)
            }
            playlistEditClient(clientId) { client ->
                newActions.forEach { action ->
                    performedActions.emit(tracks to action)
                    when (action) {
                        is Add -> {
                            client.addTracksToPlaylist(playlist, tracks, action.index, action.items)
                            tracks.addAll(action.index, action.items)
                        }

                        is Move -> {
                            client.moveTrackInPlaylist(playlist, tracks, action.from, action.to)
                            tracks.add(action.to, tracks.removeAt(action.from))
                        }

                        is Remove -> {
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
        originalList = emptyList()
        loading = null
    }


    sealed interface Action<T> {
        data class Add<T>(val index: Int, val items: MutableList<T>) : Action<T>
        data class Remove<T>(val indexes: List<Int>) : Action<T> {
            constructor(vararg indexes: Int) : this(indexes.toList())
        }

        data class Move<T>(val from: Int, val to: Int) : Action<T>
    }

    companion object {

        // I am KR bbg.
        // Don't forget to subscribe to my OnlyFans at https://github.com/justfoolingaround
        fun <T> computeActions(
            old: List<T>, new: List<T>
        ): MutableList<Action<T>> {
            val out = mutableListOf<Action<T>>()
            val before = old.toMutableList()

            // Handle removals
            val afterIds = new.map { it }.toSet()
            val removeIndexes = mutableListOf<Int>()
            var index = 0
            while (index < before.size) {
                if (before[index] !in afterIds) {
                    removeIndexes.add(index)
                    before.removeAt(index)
                } else {
                    index++
                }
            }
            if (removeIndexes.isNotEmpty()) {
                out.add(Remove(indexes = removeIndexes))
            }

            // Handle additions
            val additions = mutableListOf<T>()
            var addIndex = -1
            new.forEachIndexed { targetIndex, targetItem ->
                if (before.none { it == targetItem }) {
                    if (addIndex == -1) addIndex = targetIndex
                    additions.add(targetItem)
                    before.add(targetIndex, targetItem)
                } else if (additions.isNotEmpty()) {
                    out.add(Add(index = addIndex, items = additions.toMutableList()))
                    additions.clear()
                    addIndex = -1
                }
            }
            if (additions.isNotEmpty()) {
                out.add(Add(index = addIndex, items = additions))
            }

            // Handle moves
            val moveActions = mutableListOf<Move<T>>()
            new.forEachIndexed { targetIndex, targetItem ->
                val currentIndex = before.indexOfFirst { it == targetItem }
                if (currentIndex != -1 && currentIndex != targetIndex) {
                    moveActions.add(Move(from = currentIndex, to = targetIndex))
                    val item = before.removeAt(currentIndex)
                    before.add(targetIndex, item)
                }
            }

            //Optimize moves
            var ind = -1
            var last: Move<T>? = null
            moveActions.forEach {
                val l = last
                if (l != null) {
                    if (l.from == it.to) {
                        if (ind == -1) ind = l.from - 1
                    } else {
                        if (ind == -1) out.add(l)
                        else {
                            out.add(Move(from = ind, to = l.from))
                            ind = -1
                        }
                    }
                }
                last = it
            }
            val l = last
            if (l != null) {
                if (ind != -1) out.add(Move(from = ind, to = l.from))
                else out.add(l)
            }

            return out
        }

        fun CatchingViewModel.deletePlaylist(
            extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
            mutableMessageFlow: MutableSharedFlow<Message>,
            context: Context,
            clientId: String,
            playlist: Playlist
        ) {
            val extension = extensionListFlow.getExtension(clientId) ?: return
            viewModelScope.launch(Dispatchers.IO) {
                extension.get<PlaylistEditClient, Unit>(throwableFlow) {
                    deletePlaylist(playlist)
                }
                mutableMessageFlow.emit(Message(context.getString(R.string.playlist_deleted)))
            }
        }

        suspend fun CatchingViewModel.addToPlaylists(
            extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
            messageFlow: MutableSharedFlow<Message>,
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
                messageFlow.emit(Message(message))
            }
        }
    }
}