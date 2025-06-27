package dev.brahmkshatriya.echo.ui.main

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.ui.common.PagingUtils
import dev.brahmkshatriya.echo.ui.common.PagingUtils.collectWith
import dev.brahmkshatriya.echo.ui.common.PagingUtils.toFlow
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfAdapter.Companion.getShelfAdapter
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfClickListener
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfClickListener.Companion.getShelfListener
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.ui.UiUtils.configure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class FeedViewModel(
    val throwableFlow: MutableSharedFlow<Throwable>,
    private val extensionLoader: ExtensionLoader
) : ViewModel() {
    open val current = extensionLoader.current

    abstract suspend fun getTabs(extension: Extension<*>): Result<List<Tab>>
    abstract suspend fun getFeed(extension: Extension<*>): Result<Feed>

    val feed = MutableStateFlow<PagingUtils.Data<Shelf>>(
        PagingUtils.Data(null, null, null, null)
    )
    val loading = MutableSharedFlow<Boolean>()
    val tabs = MutableStateFlow<List<Tab>?>(null)
    var tab: Tab? = null

    fun init() {
        viewModelScope.launch {
            current.collect { refresh(true) }
        }
        viewModelScope.launch {
            extensionLoader.db.currentUsersFlow.collect { refresh(true) }
        }
    }

    private suspend fun loadTabs(extension: Extension<*>) {
        loading.emit(true)
        tabs.value = null
        val list = getTabs(extension).getOrElse {
            throwableFlow.emit(it)
            listOf()
        }
        tab = list.find { it.id == tab?.id } ?: list.firstOrNull()
        tabs.value = list
        loading.emit(false)
    }

    private var job: Job? = null
    fun refresh(reset: Boolean = false) {
        job?.cancel()
        feed.value = PagingUtils.Data(current.value, null, null, null)
        val extension = current.value ?: return
        job = viewModelScope.launch(Dispatchers.IO) {
            if (reset) loadTabs(extension)
            val data = getFeed(extension).getOrElse {
                feed.value =
                    PagingUtils.Data(extension, null, null, PagingUtils.errorPagingData(it))
                return@launch
            }.pagedData
            data.toFlow(extension).collectWith(throwableFlow) {
                feed.value = PagingUtils.Data(extension, null, data, it)
            }
        }
    }

    companion object {
        fun Fragment.configureFeed(
            viewModel: FeedViewModel,
            recyclerView: RecyclerView,
            swipeRefresh: SwipeRefreshLayout,
            tabLayout: TabLayout
        ): ShelfClickListener {
            FastScrollerHelper.applyTo(recyclerView)
            swipeRefresh.configure { viewModel.refresh(true) }
            val listener = getShelfListener()
            val adapter = getShelfAdapter(listener)
            recyclerView.adapter = adapter.withLoaders(this)
            adapter.getTouchHelper().attachToRecyclerView(recyclerView)
            observe(viewModel.feed) { (extension, _, shelf, feed) ->
                adapter.submit(extension?.id, shelf, feed)
            }
            val tabListener = object : TabLayout.OnTabSelectedListener {
                var enabled = true
                var tabs: List<Tab> = emptyList()
                override fun onTabSelected(tab: TabLayout.Tab) {
                    if (!enabled) return
                    val genre = tabs[tab.position]
                    if (viewModel.tab == genre) return
                    viewModel.tab = genre
                    viewModel.refresh()
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit
                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            }

            tabLayout.addOnTabSelectedListener(tabListener)
            observe(viewModel.tabs) {
                tabListener.enabled = it != null
                tabLayout.removeAllTabs()
                val tabs = it ?: return@observe
                tabListener.tabs = tabs
                tabLayout.isVisible = tabs.isNotEmpty()
                tabs.forEach { genre ->
                    val tab = tabLayout.newTab()
                    tab.text = genre.title
                    val selected = viewModel.tab?.id == genre.id
                    tabLayout.addTab(tab, selected)
                }
            }
            observe(viewModel.loading) { swipeRefresh.isRefreshing = it }
            return listener
        }
    }
}