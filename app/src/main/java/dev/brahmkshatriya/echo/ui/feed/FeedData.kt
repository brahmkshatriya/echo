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
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.get
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.extensions.builtin.offline.MediaStoreUtils.searchBy
import dev.brahmkshatriya.echo.extensions.exceptions.AppException.Companion.toAppException
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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

@OptIn(ExperimentalCoroutinesApi::class)
data class FeedData(
    private val feedId: String,
    private val scope: CoroutineScope,
    private val app: App,
    private val extensionLoader: ExtensionLoader,
    private val load: suspend ExtensionLoader.() -> State<Feed<Shelf>>?,
    private val defaultButtons: Feed.Buttons,
    private val noVideos: Boolean,
    private val extraLoadFlow: Flow<*>
) {
    val current = extensionLoader.current
    suspend fun getExtension(id: String) =
        extensionLoader.getFlow(ExtensionType.MUSIC).getExtensionOrThrow(id)

    val layoutManagerStates = hashMapOf<Int, Parcelable?>()
    val visibleScrollableViews = hashMapOf<Int, WeakReference<HorizontalListViewHolder>>()

    private val refreshFlow = MutableSharedFlow<Unit>(1)
    private val feedState = MutableStateFlow<Result<State<Feed<Shelf>>?>?>(null)
    private val selectedTabFlow = MutableStateFlow<Tab?>(null)

    val loadedShelves = MutableStateFlow<List<Shelf>?>(null)
    var searchToggled: Boolean = false
    var searchQuery: String? = null
    val feedSortState = MutableStateFlow<FeedSort.State?>(null)
    val searchClickedFlow = MutableSharedFlow<Unit>()

    private val dataFlow = feedState.combine(selectedTabFlow) { feed, tab ->
        if (feed == null) return@combine null
        getData(feed, tab)
    }.stateIn(scope, Lazily, null)

    private fun getData(
        state: Result<State<Feed<Shelf>>?>, tab: Tab?
    ) = scope.async(Dispatchers.IO, CoroutineStart.LAZY) {
        runCatching {
            val (extensionId, item, feed) = state.getOrThrow() ?: return@runCatching null
            val extension = getExtension(extensionId)
            State(
                extensionId,
                item,
                extension.get {
                    val data = feed.getPagedData(tab)
                    data.copy(
                        data.pagedData.map { result ->
                            result.getOrElse {
                                throw it.toAppException(extension)
                            }
                        }
                    )
                }.getOrThrow()
            )
        }
    }

    val shouldShowEmpty = dataFlow.transformLatest {
        emit(false)
        emit(it?.await()?.getOrNull() != null)
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

    val buttonsFlow = dataFlow.combineTransformLatest(feedSortState) { it, state ->
        emit(null)
        val feed = it?.await()?.getOrNull() ?: return@combineTransformLatest
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

    val backgroundImageFlow = dataFlow.transformLatest {
        emit(null)
        val drawable = it?.await()?.getOrNull()?.feed?.background ?: return@transformLatest
        emit(drawable.loadDrawable(app.context))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingFlow = dataFlow.flatMapLatest { pair ->
        val extensionId = feedState.value?.getOrNull()?.extensionId
        searchQuery = null
        searchToggled = false
        feedSortState.value = extensionId?.let { app.context.getFromCache("${feedId}_$it", "sort") }
        loadedShelves.value = null
        if (pair != null) merge(feedSortState, searchClickedFlow).flatMapLatest {
            PagedSource(getFeedSourceData(pair)).flow
        } else PagedSource.emptyFlow()
    }.cachedIn(scope)


    private suspend fun getFeedSourceData(
        pair: Deferred<Result<State<Feed.Data<Shelf>>?>>
    ): Deferred<Result<PagedData<FeedType>?>> {
        val tabId = selectedTabFlow.value?.id
        val data = if (feedSortState.value != null || searchQuery != null) {
            val result = pair.await().mapCatching { state ->
                state ?: return@mapCatching null
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
                    shelves = sortState.feedSort?.sorter?.invoke(shelves) ?: shelves
                    if (sortState.reversed) shelves = shelves.reversed()
                    if (sortState.save)
                        app.context.saveToCache("${feedId}_$extensionId", sortState, "sort")
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
            scope.async(Dispatchers.IO, CoroutineStart.LAZY) { result }
        } else scope.async(Dispatchers.IO, CoroutineStart.LAZY) {
            pair.await().map { state ->
                state ?: return@map null
                val extensionId = state.extensionId
                val data = state.feed.pagedData
                data.map { result ->
                    result.map {
                        it.toFeedType(feedId, extensionId, state.item, tabId, noVideos)
                    }.getOrThrow()
                }
            }
        }
        return data
    }

    private suspend fun <T : Any> PagedData<T>.loadTill(limit: Long): List<T> {
        val list = mutableListOf<T>()
        var page = loadList(null)
        list.addAll(page.data)
        while (page.continuation != null && list.size < limit) {
            page = loadList(page.continuation)
            list.addAll(page.data)
        }
        return list
    }

    val isRefreshing get() = feedState.value == null
    val isRefreshingFlow = feedState.map { isRefreshing }

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
            listOfNotNull(current, refreshFlow, extraLoadFlow)
                .merge().collectLatest {
                    feedState.value = null
                    extensionLoader.current.value ?: return@collectLatest
                    feedState.value = runCatching { load(extensionLoader) }
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