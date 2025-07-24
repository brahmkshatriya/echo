package dev.brahmkshatriya.echo.ui.player.more.lyrics

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.LyricsSearchClient
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getAs
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getIf
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.extensions.exceptions.AppException.Companion.toAppException
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.common.PagedSource
import dev.brahmkshatriya.echo.ui.extensions.list.ExtensionListViewModel
import dev.brahmkshatriya.echo.utils.CoroutineUtils.combineTransformLatest
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

class LyricsViewModel(
    private val app: App,
    extensionLoader: ExtensionLoader,
    playerState: PlayerState
) : ExtensionListViewModel<Extension<*>>() {

    override val currentSelectionFlow = MutableStateFlow<Extension<*>?>(null)

    val queryFlow = MutableStateFlow("")
    val selectedTabIndexFlow = MutableStateFlow(-1)
    val lyricsState = MutableStateFlow<State>(State.Empty)

    private val mediaFlow = playerState.current.map { current ->
        current?.mediaItem?.takeIf { it.isLoaded }
    }.distinctUntilChanged().stateIn(viewModelScope, Eagerly, null)

    override val extensionsFlow = extensionLoader.lyrics.combine(mediaFlow) { lyrics, mediaItem ->
        val trackExtension = mediaItem?.extensionId?.let { id ->
            extensionLoader.music.getExtension(id)?.takeIf { it.isClient<LyricsClient>() }
        }
        listOfNotNull(trackExtension) + lyrics
    }.onEach { extensions ->
        val id = app.settings.getString(LAST_LYRICS_KEY, null)
        val extension = extensions.find { it.id == id } ?: extensions.firstOrNull()
        currentSelectionFlow.value = null
        queryFlow.value = ""
        currentSelectionFlow.value = extension
        lyricsState.value = State.Loading
    }.stateIn(viewModelScope, Eagerly, emptyList())

    private val feedData = currentSelectionFlow.combineTransformLatest(queryFlow) { e, q ->
        emit(null)
        if (e == null) return@combineTransformLatest
        val item = mediaFlow.value ?: return@combineTransformLatest
        val result = viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
            if (q.isEmpty()) e.getAs<LyricsClient, Feed<Lyrics>> {
                searchTrackLyrics(item.extensionId, item.track).toApp(e)
            } else e.getAs<LyricsSearchClient, Feed<Lyrics>> {
                searchLyrics(q).toApp(e)
            }
        }
        emit(result)
    }.stateIn(viewModelScope, Eagerly, null)

    fun <T : Any> Feed<T>.toApp(extension: Extension<*>) = Feed(tabs) { tab ->
        extension.get {
            val data = getPagedData(tab)
            data.copy(
                data.pagedData.map { result ->
                    result.getOrElse { throw it.toAppException(extension) }
                }
            )
        }.getOrThrow()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val tabsFlow = feedData.transformLatest {
        selectedTabIndexFlow.value = 0
        emit(emptyList())
        emit(it?.await()?.getOrNull()?.tabs.orEmpty())
    }.stateIn(viewModelScope, Eagerly, emptyList())

    private val pagedDataFlow =
        feedData.combineTransformLatest(selectedTabIndexFlow) { result, index ->
            emit(null)
            if (result == null) return@combineTransformLatest
            emit(viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
                result.await().mapCatching {
                    val data =
                        it.getPagedData(it.tabs.run { getOrNull(index) ?: firstOrNull() }).pagedData
                    if (queryFlow.value.isEmpty()) onLyricsSelected(data.loadList(null).data.firstOrNull())
                    data
                }
            })
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, Eagerly, null)

    val shouldShowEmpty = pagedDataFlow.map { it != null }
        .stateIn(viewModelScope, Eagerly, false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingFlow = pagedDataFlow.flatMapLatest { result ->
        if (result == null) PagedSource.emptyFlow()
        else PagedSource(result).flow
    }.cachedIn(viewModelScope)

    sealed interface State {
        data object Loading : State
        data object Empty : State
        data class Loaded(val lyrics: Lyrics) : State
    }

    fun onLyricsSelected(lyricsItem: Lyrics?) {
        val lyrics = lyricsItem ?: return
        val extension = currentSelectionFlow.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            lyricsState.value = State.Loading
            lyricsState.value = extension.getIf<LyricsClient, Lyrics>(app.throwFlow) {
                loadLyrics(lyrics)
            }?.fillGaps()?.let { State.Loaded(it) } ?: State.Empty
        }
    }

    private fun Lyrics.fillGaps(): Lyrics {
        val lyrics = this.lyrics as? Lyrics.Timed
        return if (lyrics != null && lyrics.fillTimeGaps) {
            val new = mutableListOf<Lyrics.Item>()
            var last = 0L
            lyrics.list.forEach {
                if (it.startTime > last) {
                    new.add(Lyrics.Item("", last, it.startTime))
                }
                new.add(it)
                last = it.endTime
            }
            this.copy(lyrics = Lyrics.Timed(new))
        } else this
    }

    companion object {
        const val LAST_LYRICS_KEY = "last_lyrics_client"
    }
}