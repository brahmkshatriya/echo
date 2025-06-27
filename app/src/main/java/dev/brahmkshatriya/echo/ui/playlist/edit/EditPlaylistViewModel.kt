package dev.brahmkshatriya.echo.ui.playlist.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditorListenerClient
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.run
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class EditPlaylistViewModel(
    private val extensionId: String,
    var playlist: Playlist,
    private val loaded: Boolean,
    private val app: App,
    extensionLoader: ExtensionLoader,
) : ViewModel() {

    val playlistName = MutableStateFlow(playlist.title)
    val playlistDescription = MutableStateFlow(playlist.description)

    private val extensions = extensionLoader.music

    sealed interface SaveState {
        data object Initial : SaveState
        data class Performing(val action: Action<Track>, val tracks: List<Track>) : SaveState
        data object Saving : SaveState
        data class Saved(val success: Boolean) : SaveState
    }

    val saveState = MutableStateFlow<SaveState>(SaveState.Initial)
    fun save() = viewModelScope.launch(Dispatchers.IO) {
        val success = runCatching {
            saveState.value = SaveState.Saving

            val extension = extensions.getExtensionOrThrow(extensionId)
            if (playlist.title != playlistName.value || playlist.description != playlistDescription.value) {
                extension.get<PlaylistEditClient, Unit> {
                    editPlaylistMetadata(playlist, playlistName.value, playlistDescription.value)
                }.getOrThrow()
            }
            val original = (originalList.value as LoadingState.Loaded).list!!
            val newActions = computeActions(original, currentTracks.value!!)
            newActions.ifEmpty { return@runCatching true }

            var tracks = original

            extension.run<PlaylistEditorListenerClient, Unit> {
                onEnterPlaylistEditor(playlist, tracks)
            }.getOrThrow()

            extension.get<PlaylistEditClient, Unit> {
                newActions.forEach { action ->
                    when (action) {
                        is Action.Add -> {
                            saveState.value = SaveState.Performing(action, action.items)
                            addTracksToPlaylist(playlist, tracks, action.index, action.items)
                            tracks = loadTracks(playlist).loadAll()
                        }

                        is Action.Move -> {
                            saveState.value =
                                SaveState.Performing(action, listOf(tracks[action.from]))
                            moveTrackInPlaylist(playlist, tracks, action.from, action.to)
                            tracks = tracks.toMutableList().apply {
                                add(action.to, removeAt(action.from))
                            }
                        }

                        is Action.Remove -> {
                            saveState.value =
                                SaveState.Performing(action, action.indexes.map { tracks[it] })
                            removeTracksFromPlaylist(playlist, tracks, action.indexes)
                            tracks = loadTracks(playlist).loadAll()
                        }
                    }
                }
            }.getOrThrow()

            extension.run<PlaylistEditorListenerClient, Unit> {
                onExitPlaylistEditor(playlist, tracks)
            }.getOrThrow()
            true
        }.getOrElse {
            app.throwFlow.emit(it)
            false
        }
        saveState.value = SaveState.Saved(success)
    }

    sealed interface LoadingState {
        data object Loading : LoadingState
        data class Loaded(val list: List<Track>?) : LoadingState
    }

    val originalList = MutableStateFlow<LoadingState>(LoadingState.Loading)
    val currentTracks = MutableStateFlow<List<Track>?>(null)
    fun edit(action: Action<Track>) {
        currentTracks.value = currentTracks.value?.toMutableList()?.apply {
            runCatching {
                when (action) {
                    is Action.Add -> addAll(action.index, action.items)
                    is Action.Move -> add(action.to, removeAt(action.from))
                    is Action.Remove -> action.indexes.forEach { removeAt(it) }
                }
            }.getOrElse {
                viewModelScope.launch { app.throwFlow.emit(it) }
            }
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val tracks = runCatching {
                val extension = extensions.getExtensionOrThrow(extensionId)
                extension.get<PlaylistEditClient, List<Track>> {
                    if (!loaded) playlist = loadPlaylist(playlist)
                    loadTracks(playlist).loadAll()
                }.getOrThrow()
            }.getOrElse {
                app.throwFlow.emit(it)
                null
            }
            originalList.value = LoadingState.Loaded(tracks)
            currentTracks.value = tracks
        }
    }

    sealed interface Action<T> {
        data class Add<T>(val index: Int, val items: MutableList<T>) : Action<T>
        data class Remove<T>(val indexes: List<Int>) : Action<T>
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
                out.add(Action.Remove(indexes = removeIndexes))
            }

            val additions = mutableListOf<T>()
            var addIndex = -1
            new.forEachIndexed { targetIndex, targetItem ->
                if (before.none { it == targetItem }) {
                    if (addIndex == -1) addIndex = targetIndex
                    additions.add(targetItem)
                    before.add(targetIndex, targetItem)
                } else if (additions.isNotEmpty()) {
                    out.add(Action.Add(index = addIndex, items = additions.toMutableList()))
                    additions.clear()
                    addIndex = -1
                }
            }
            if (additions.isNotEmpty()) {
                out.add(Action.Add(index = addIndex, items = additions))
            }

            val moveActions = mutableListOf<Action.Move<T>>()
            new.forEachIndexed { targetIndex, targetItem ->
                val currentIndex = before.indexOfFirst { it == targetItem }
                if (currentIndex != -1 && currentIndex != targetIndex) {
                    moveActions.add(Action.Move(from = currentIndex, to = targetIndex))
                    val item = before.removeAt(currentIndex)
                    before.add(targetIndex, item)
                }
            }

            var ind = -1
            var last: Action.Move<T>? = null
            moveActions.forEach {
                val l = last
                if (l != null) {
                    if (l.from == it.to) {
                        if (ind == -1) ind = l.from - 1
                    } else {
                        if (ind == -1) out.add(l)
                        else {
                            out.add(Action.Move(from = ind, to = l.from))
                            ind = -1
                        }
                    }
                }
                last = it
            }
            val l = last
            if (l != null) {
                if (ind != -1) out.add(Action.Move(from = ind, to = l.from))
                else out.add(l)
            }

            return out
        }
    }
}
