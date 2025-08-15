package dev.brahmkshatriya.echo.ui.feed

import android.os.Parcelable
import androidx.paging.cachedIn
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.di.App
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.extensions.builtin.offline.MediaStoreUtils.searchBy
import dev.brahmkshatriya.echo.ui.common.PagedSource
import dev.brahmkshatriya.echo.ui.feed.FeedType.Companion.toFeedType
import dev.brahmkshatriya.echo.ui.feed.viewholders.HorizontalListViewHolder
import dev.brahmkshatriya.echo.utils.CacheUtils.getFromCache
import dev.brahmkshatriya.echo.utils.CacheUtils.saveToCache
import dev.brahmkshatriya.echo.utils.CoroutineUtils.combineTransformLatest
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadDrawable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
data class FeedData(
    private val feedId: String,
    private val scope: CoroutineScope,
    private val app: App,
    private val extensionLoader: ExtensionLoader,
    private val cached: suspend ExtensionLoader.() -> State<Feed<Shelf>>?,
    private val load: suspend ExtensionLoader.() -> State<Feed<Shelf>>?,
    private val defaultButtons: Feed.Buttons,
    private val noVideos: Boolean,
    private val extraLoadFlow: Flow<*>
) {
    val current = extensionLoader.current
    val usersFlow = extensionLoader.db.currentUsersFlow
    suspend fun getExtension(id: String) =
        extensionLoader.getFlow(ExtensionType.MUSIC).getExtensionOrThrow(id)

    val layoutManagerStates = hashMapOf<Int, Parcelable?>()
    val visibleScrollableViews = hashMapOf<Int, WeakReference<HorizontalListViewHolder>>()

    private val refreshFlow = MutableSharedFlow<Unit>(1)
    private val cachedState = MutableStateFlow<Result<State<Feed<Shelf>>?>?>(null)
    private val loadedState = MutableStateFlow<Result<State<Feed<Shelf>>?>?>(null)
    private val selectedTabFlow = MutableStateFlow<Tab?>(null)

    val loadedShelves = MutableStateFlow<List<Shelf>?>(null)
    var searchToggled: Boolean = false
    var searchQuery: String? = null
    val feedSortState = MutableStateFlow<FeedSort.State?>(null)
    val searchClickedFlow = MutableSharedFlow<Unit>()

    private val feedState = cachedState.combine(loadedState) { cached, loaded ->
        loaded ?: cached
    }.stateIn(scope, Lazily, null)

    private val cachedDataFlow = cachedState.combine(selectedTabFlow) { feed, tab ->
        if (feed == null) return@combine null
        getData(feed, tab)
    }.stateIn(scope, Lazily, null)

    private val loadedDataFlow = loadedState.combine(selectedTabFlow) { feed, tab ->
        if (feed == null) return@combine null
        getData(feed, tab)
    }.stateIn(scope, Lazily, null)

    private fun getData(
        state: Result<State<Feed<Shelf>>?>, tab: Tab?
    ) = scope.async(Dispatchers.IO, CoroutineStart.LAZY) {
        runCatching {
            val (extensionId, item, feed) = state.getOrThrow() ?: return@runCatching null
            State(extensionId, item, feed.getPagedData(tab))
        }
    }

    val dataFlow = cachedDataFlow.combine(loadedDataFlow) { cached, loaded ->
        val extensionId = feedState.value?.getOrNull()?.extensionId
        val tabId = selectedTabFlow.value?.id
        searchQuery = null
        searchToggled = false
        val id = "$extensionId-$feedId-$tabId"
        feedSortState.value = extensionId?.let { app.context.getFromCache(id, "sort") }
        loadedShelves.value = null
        cached to loaded
    }

    val shouldShowEmpty = dataFlow.transformLatest { (cached, loaded) ->
        emit(false)
        val data = loaded ?: cached
        emit(data?.await()?.getOrNull() != null)
    }.stateIn(scope, Lazily, false)

    val tabsFlow = feedState.map { pair ->
        val state = pair?.getOrNull() ?: return@map listOf()
        state.feed.tabs.map {
            FeedTab(feedId, state.extensionId, it)
        }
    }

    val selectedTabIndexFlow = tabsFlow.combine(selectedTabFlow) { tabs, tab ->
        tabs.indexOfFirst { it.tab.id == tab?.id }
    }

    data class FeedTab(
        val feedId: String,
        val extensionId: String,
        val tab: Tab
    )

    data class Buttons(
        val feedId: String,
        val extensionId: String,
        val buttons: Feed.Buttons,
        val item: EchoMediaItem? = null,
        val sortState: FeedSort.State? = null,
    )

    val buttonsFlow = dataFlow.combineTransformLatest(feedSortState) { data, state ->
        emit(null)
        suspend fun emitFeed(feed: Deferred<Result<State<Feed.Data<Shelf>>?>>?) {
            val feed = feed?.await()?.getOrNull() ?: return
            emit(
                Buttons(
                    feedId,
                    feed.extensionId,
                    feed.feed.buttons ?: defaultButtons,
                    feed.item,
                    state,
                )
            )
        }
        emitFeed(data.first)
        emitFeed(data.second)
    }

    val backgroundImageFlow = dataFlow.transformLatest { (cached, loaded) ->
        emit(null)
        val cachedDrawable =
            cached?.await()?.getOrNull()?.feed?.background ?: return@transformLatest
        emit(cachedDrawable.loadDrawable(app.context))
        val loadedDrawable =
            loaded?.await()?.getOrNull()?.feed?.background ?: return@transformLatest
        if (loadedDrawable != cachedDrawable) {
            emit(loadedDrawable.loadDrawable(app.context))
        }
    }.stateIn(scope, Lazily, null)


    val cachedFeedTypeFlow =
        combineTransformLatest(cachedDataFlow, feedSortState, searchClickedFlow) { _ ->
            emit(null)
            val cached = cachedDataFlow.value ?: return@combineTransformLatest
            emit(getFeedSourceData(cached))
        }.stateIn(scope, Lazily, null)

    val loadedFeedTypeFlow =
        combineTransformLatest(loadedDataFlow, feedSortState, searchClickedFlow) { _ ->
            emit(null)
            val loaded = loadedDataFlow.value ?: return@combineTransformLatest
            emit(getFeedSourceData(loaded))
        }.stateIn(scope, Lazily, null)

    val pagingFlow =
        cachedFeedTypeFlow.combineTransformLatest(loadedFeedTypeFlow) { cached, loaded ->
            emitAll(PagedSource(loaded, cached).flow)
        }.cachedIn(scope)

    private suspend fun getFeedSourceData(
        deferred: Deferred<Result<State<Feed.Data<Shelf>>?>>
    ): Result<PagedData<FeedType>> {
        val tabId = selectedTabFlow.value?.id
        val data = if (feedSortState.value != null || searchQuery != null) {
            val result = deferred.await().mapCatching { state ->
                state ?: return@mapCatching PagedData.empty()
                val extensionId = state.extensionId
                val data = state.feed.pagedData

                val sortState = feedSortState.value
                val query = searchQuery
                var shelves = data.loadTill(2000)
                shelves = if (sortState?.feedSort != null || query != null)
                    shelves.flatMap { shelf ->
                        when (shelf) {
                            is Shelf.Category -> listOf(shelf)
                            is Shelf.Item -> listOf(shelf)
                            is Shelf.Lists.Categories -> shelf.list
                            is Shelf.Lists.Items -> shelf.list.map { it.toShelf() }
                            is Shelf.Lists.Tracks -> shelf.list.map { it.toShelf() }
                        }
                    }
                else shelves
                loadedShelves.value = shelves
                if (sortState != null) {
                    shelves = sortState.feedSort?.sorter?.invoke(app.context, shelves) ?: shelves
                    if (sortState.reversed) shelves = shelves.reversed()
                    if (sortState.save)
                        app.context.saveToCache("$extensionId-$feedId-$tabId", sortState, "sort")
                }
                if (query != null) {
                    shelves = shelves.searchBy(query) {
                        listOf(it.title)
                    }.map { it.second }
                }
                PagedData.Single {
                    shelves.toFeedType(
                        feedId,
                        extensionId,
                        state.item,
                        tabId,
                        noVideos
                    )
                }
            }
            result
        } else deferred.await().mapCatching { state ->
            state ?: return@mapCatching PagedData.empty()
            val extensionId = state.extensionId
            val data = state.feed.pagedData
            data.loadPage(null)
            data.map { result ->
                result.map {
                    it.toFeedType(feedId, extensionId, state.item, tabId, noVideos)
                }.getOrThrow()
            }
        }
        return data
    }

    private suspend fun <T : Any> PagedData<T>.loadTill(limit: Long): List<T> {
        val list = mutableListOf<T>()
        var page = loadPage(null)
        list.addAll(page.data)
        while (page.continuation != null && list.size < limit) {
            page = loadPage(page.continuation)
            list.addAll(page.data)
        }
        return list
    }

    val isRefreshing get() = loadedState.value?.getOrNull() != null && loadedFeedTypeFlow.value == null
    val isRefreshingFlow = loadedFeedTypeFlow.map {
        isRefreshing
    }

    fun selectTab(extensionId: String?, pos: Int) {
        val state = feedState.value?.getOrNull()
        val tab = state?.feed?.tabs?.getOrNull(pos)
            ?.takeIf { state.extensionId == extensionId }
        app.context.saveToCache(feedId, tab?.id, "selected_tab")
        selectedTabFlow.value = tab
    }

    fun refresh() = scope.launch { refreshFlow.emit(Unit) }

    init {
        scope.launch {
            listOfNotNull(current, refreshFlow, usersFlow, extraLoadFlow)
                .merge().debounce(100L).collectLatest {
                    cachedState.value = null
                    loadedState.value = null
                    extensionLoader.current.value ?: return@collectLatest
                    cachedState.value = runCatching { cached(extensionLoader) }
                    loadedState.value = runCatching { load(extensionLoader) }
                }
        }
        scope.launch {
            feedState.collect { result ->
                val feed = result?.getOrNull()?.feed?.tabs
                selectedTabFlow.value = if (feed == null) null else {
                    val last = app.context.getFromCache<String>(feedId, "selected_tab")
                    feed.find { it.id == last } ?: feed.firstOrNull()
                }
            }
        }
    }

    data class State<T>(
        val extensionId: String,
        val item: EchoMediaItem?,
        val feed: T,
    )

    fun onSearchClicked() = scope.launch { searchClickedFlow.emit(Unit) }
}