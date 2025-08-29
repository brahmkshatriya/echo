package dev.brahmkshatriya.echo.ui.player.more.lyrics

import androidx.core.content.edit
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.paging.cachedIn
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.extensions.cache.Cached
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.common.PagedSource
import dev.brahmkshatriya.echo.ui.extensions.list.ExtensionListViewModel
import dev.brahmkshatriya.echo.utils.CacheUtils.getFromCache
import dev.brahmkshatriya.echo.utils.CacheUtils.saveToCache
import dev.brahmkshatriya.echo.utils.CoroutineUtils.combineTransformLatest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

class LyricsViewModel(
    private val app: App,
    extensionLoader: ExtensionLoader,
    playerState: PlayerState,
) : ExtensionListViewModel<Extension<*>>() {

    private val refreshFlow = MutableSharedFlow<Unit>()
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
        currentSelectionFlow.value = null
        lyricsState.value = State.Loading
        queryFlow.value = ""
        val media = mediaFlow.value?.track?.id
        currentSelectionFlow.value = media?.let {
            val id = app.context.getFromCache<String>(media, "lyrics_ext")
                ?: app.settings.getString(LAST_LYRICS_KEY, null)
            extensions.find { it.id == id } ?: extensions.firstOrNull()
        }
        refreshFlow.emit(Unit)
    }.stateIn(viewModelScope, Eagerly, emptyList())

    override fun onExtensionSelected(extension: Extension<*>) {
        app.settings.edit { putString(LAST_LYRICS_KEY, extension.id) }
        val media = mediaFlow.value?.track?.id ?: return
        app.context.saveToCache<String>(media, extension.id, "lyrics_ext")
    }

    fun reloadCurrent() = viewModelScope.launch { refreshFlow.emit(Unit) }

    private val cachedFeed = combineTransformLatest(
        currentSelectionFlow, mediaFlow, queryFlow, refreshFlow
    ) {
        emit(null)
        val extension = it[0] as Extension<*>? ?: return@combineTransformLatest
        val item = it[1] as MediaItem? ?: return@combineTransformLatest
        val query = it[2] as String
        val result = Cached.getLyricsFeed(app, extension.id, item.extensionId, item.track, query)
        emit(result)
    }.stateIn(viewModelScope, Eagerly, null)

    private val loadedFeed = combineTransformLatest(
        currentSelectionFlow, mediaFlow, queryFlow, refreshFlow
    ) {
        emit(null)
        val extension = it[0] as Extension<*>? ?: return@combineTransformLatest
        val item = it[1] as MediaItem? ?: return@combineTransformLatest
        val query = it[2] as String
        val result = Cached.loadLyricsFeed(app, extension, item.extensionId, item.track, query)
        emit(result)
    }.stateIn(viewModelScope, Eagerly, null)

    private val feedData = loadedFeed.combine(cachedFeed) { loaded, cache ->
        loaded ?: cache
    }.stateIn(viewModelScope, Eagerly, null)

    val tabsFlow = feedData.map {
        selectedTabIndexFlow.value = 0
        it?.getOrNull()?.tabs.orEmpty()
    }.stateIn(viewModelScope, Eagerly, emptyList())

    private val pagedDataFlow =
        feedData.combineTransformLatest(selectedTabIndexFlow) { result, index ->
            emit(null)
            if (result == null) {
                lyricsState.value = State.Loading
                return@combineTransformLatest
            }
            emit(result.mapCatching {
                val data =
                    it.getPagedData(it.tabs.run { getOrNull(index) ?: firstOrNull() }).pagedData
                if (queryFlow.value.isEmpty()) onLyricsSelected(data.loadPage(null).data.firstOrNull())
                data
            })
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, Eagerly, null)

    val shouldShowEmpty = pagedDataFlow.map { it != null }
        .stateIn(viewModelScope, Eagerly, false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingFlow = pagedDataFlow.transformLatest { result ->
        emit(PagedSource.empty())
        emitAll(PagedSource(result).flow)
    }.cachedIn(viewModelScope)

    sealed interface State {
        data object Empty : State
        data object Loading : State
        data class Loaded(val result: Result<Lyrics>) : State
    }

    fun onLyricsSelected(lyricsItem: Lyrics?) = viewModelScope.launch(Dispatchers.IO) {
        lyricsState.value = State.Loading
        if (lyricsItem == null) lyricsState.value = State.Empty else {
            val extension = currentSelectionFlow.value ?: return@launch
            lyricsState.value =
                State.Loaded(Cached.loadLyrics(app, extension, lyricsItem).map { it.fillGaps() })
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