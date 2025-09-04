package dev.brahmkshatriya.echo.ui.playlist.edit

import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditCoverClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditorListenerClient
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toResourceUriImageHolder
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getAs
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getIf
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getOrThrow
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.extensions.InstallationUtils.openFileSelector
import dev.brahmkshatriya.echo.utils.CoroutineUtils.combineTransformLatest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class EditPlaylistViewModel(
    extensionLoader: ExtensionLoader,
    private val app: App,
    private val extensionId: String,
    private val initial: Playlist,
    private val loaded: Boolean,
    private val selectedTab: String?,
    private val removeIndex: Int,
) : ViewModel() {
    val extensionFlow = extensionLoader.music.map { list -> list.find { it.id == extensionId } }
        .stateIn(viewModelScope, Eagerly, null)

    val playlistFlow = extensionFlow.transformLatest { extension ->
        emit(null)
        if (extension == null) return@transformLatest
        emit(extension.getAs<PlaylistClient, Playlist> {
            if (!loaded) loadPlaylist(initial) else initial
        })
    }.stateIn(viewModelScope, Eagerly, null)

    private val feedFlow = playlistFlow.transformLatest { playlist ->
        emit(null)
        val extension = extensionFlow.value ?: return@transformLatest
        if (playlist == null) return@transformLatest
        emit(extension.getAs<PlaylistClient, Feed<Track>> {
            loadTracks(playlist.getOrThrow())
        })
    }

    val selectedTabFlow = MutableStateFlow<Tab?>(null)
    val tabsFlow = feedFlow.map { result ->
        val tabs = result?.getOrNull()?.tabs ?: emptyList()
        selectedTabFlow.value = tabs.find { it.id == selectedTab } ?: tabs.firstOrNull()
        tabs
    }.stateIn(viewModelScope, Eagerly, emptyList())

    val originalList = feedFlow.combineTransformLatest(selectedTabFlow) { result, tab ->
        emit(null)
        if (result == null) return@combineTransformLatest
        val extension = extensionFlow.value ?: return@combineTransformLatest
        val tracks = extension.getAs<PlaylistClient, List<Track>> {
            result.getOrThrow().getPagedData(tab).pagedData.loadAll()
        }
        emit(tracks)
    }.stateIn(viewModelScope, Eagerly, null)

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val newActions = currentTracks.transformLatest { current ->
        emit(null)
        val list = originalList.value?.getOrNull()
        if (list == null || current == null) return@transformLatest
        emit(computeActions(list, current))
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, Eagerly, null)

    data class Data(
        val title: String,
        val desc: String?,
        val coverEditable: Boolean,
        val cover: ImageHolder?,
    )

    val nameFlow = MutableStateFlow(initial.title)
    val descriptionFlow = MutableStateFlow(initial.description)

    sealed interface CoverState {
        data object Initial : CoverState
        data object Removed : CoverState
        data class Changed(val file: File) : CoverState
    }

    val coverFlow = MutableStateFlow<CoverState>(CoverState.Initial)
    val dataFlow = combineTransformLatest(nameFlow, descriptionFlow, coverFlow, extensionFlow) {
        val title = it[0] as String
        val desc = it[1] as String?
        val cover = it[2] as CoverState
        val extension = it[3] as Extension<*>?
        emit(Data(title, desc, false, null))
        val isEditable = extension?.isClient<PlaylistEditCoverClient>() == true
        val image = when (cover) {
            is CoverState.Changed -> cover.file.toUri().toString().toResourceUriImageHolder()
            CoverState.Initial -> initial.cover
            CoverState.Removed -> null
        }
        emit(Data(title, desc, isEditable, image))
    }

    sealed interface SaveState {
        data object Initial : SaveState
        data class Performing(val action: Action<Track>, val tracks: List<Track>) : SaveState
        data object Saving : SaveState
        data class Saved(val result: Result<Unit>) : SaveState
    }

    private val saveFlow = MutableSharedFlow<Unit>()
    val isSaveable = newActions.combine(dataFlow) { actions, pair ->
        val playlist = playlistFlow.value?.getOrNull() ?: return@combine false
        if (playlist.title != pair.title) return@combine true
        if (playlist.description != pair.desc) return@combine true
        if (coverFlow.value != CoverState.Initial) return@combine true
        !actions.isNullOrEmpty()
    }

    val saveState = saveFlow.transformLatest {
        emit(SaveState.Saving)
        val saved = SaveState.Saved(runCatching {
            val playlist = playlistFlow.value!!.getOrThrow()
            val extension = extensionFlow.value!!
            if (playlist.title != nameFlow.value || playlist.description != descriptionFlow.value) {
                extension.getAs<PlaylistEditClient, Unit> {
                    editPlaylistMetadata(playlist, nameFlow.value, descriptionFlow.value)
                }.getOrThrow()
            }
            val cover = coverFlow.value
            when (cover) {
                CoverState.Initial -> {}
                CoverState.Removed -> extension.getAs<PlaylistEditCoverClient, Unit> {
                    editPlaylistCover(playlist, null)
                }

                is CoverState.Changed -> extension.getAs<PlaylistEditCoverClient, Unit> {
                    editPlaylistCover(playlist, cover.file)
                }
            }

            val newActions = newActions.value!!
            if (newActions.isEmpty()) return@runCatching

            var tracks = originalList.value!!.getOrThrow()
            val selectedTab = selectedTabFlow.value
            extension.getIf<PlaylistEditorListenerClient, Unit> {
                onEnterPlaylistEditor(playlist, tracks)
            }.getOrThrow()

            extension.getAs<PlaylistEditClient, Unit> {
                newActions.forEach { action ->
                    when (action) {
                        is Action.Add -> {
                            addTracksToPlaylist(playlist, tracks, action.index, action.items)
                            tracks =
                                loadTracks(playlist).run { getPagedData(selectedTab).pagedData }
                                    .loadAll()
                        }

                        is Action.Move -> {
                            moveTrackInPlaylist(playlist, tracks, action.from, action.to)
                            tracks = tracks.toMutableList().apply {
                                add(action.to, removeAt(action.from))
                            }
                        }

                        is Action.Remove -> {
                            removeTracksFromPlaylist(playlist, tracks, action.indexes)
                            tracks =
                                loadTracks(playlist).run { getPagedData(selectedTab).pagedData }
                                    .loadAll()
                        }
                    }
                }
            }.getOrThrow()
            extension.getIf<PlaylistEditorListenerClient, Unit> {
                onExitPlaylistEditor(playlist, tracks)
            }.getOrThrow()
        })
        saved.result.getOrThrow(app.throwFlow)
        emit(saved)
    }.stateIn(viewModelScope, Eagerly, SaveState.Initial)

    fun save() = viewModelScope.launch { saveFlow.emit(Unit) }
    fun changeCover(activity: FragmentActivity) = viewModelScope.launch {
        runCatching {
            coverFlow.value = CoverState.Changed(activity.openFileSelector(fileType = "image/*"))
        }.getOrThrow(app.throwFlow)
    }

    init {
        viewModelScope.launch {
            originalList.collectLatest {
                currentTracks.value = it?.getOrNull()
            }
        }
        viewModelScope.launch {
            if (removeIndex == -1) return@launch
            currentTracks.first { it != null }
            edit(Action.Remove(listOf(removeIndex)))
            newActions.first { it != null }
            saveFlow.emit(Unit)
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
            old: List<T>, new: List<T>,
        ): MutableList<Action<T>> {
            val out = mutableListOf<Action<T>>()
            val before = old.toMutableList()

            val afterIds = new.map { it }.toSet()
            val removeIndexes = before.mapIndexedNotNull { index, item ->
                if (item !in afterIds) index else null
            }

            if (removeIndexes.isNotEmpty()) {
                removeIndexes.sortedDescending().forEach { before.removeAt(it) }
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
