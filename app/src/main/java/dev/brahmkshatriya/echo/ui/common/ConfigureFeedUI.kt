package dev.brahmkshatriya.echo.ui.common

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.ui.adapter.ShelfAdapter
import dev.brahmkshatriya.echo.utils.collect
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.ui.configure
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.applyAdapter
import kotlinx.coroutines.Job

inline fun <reified T> Fragment.applyClient(
    recyclerView: RecyclerView,
    swipeRefresh: SwipeRefreshLayout,
    id: Int,
    extension: Extension<*>?
): ShelfAdapter? {
    swipeRefresh.isEnabled = extension != null
    extension ?: return null
    val parent = parentFragment as Fragment
    val adapter = ShelfAdapter(
        parent,
        id.toString(),
        extension
    )
    val concatAdapter = adapter.withLoaders()
    recyclerView.applyAdapter<T>(extension, id, concatAdapter)
    return adapter
}

inline fun <reified T> Fragment.configureFeedUI(
    id: Int,
    viewModel: FeedViewModel,
    recyclerView: RecyclerView,
    swipeRefresh: SwipeRefreshLayout,
    tabLayout: TabLayout,
    clientId: String? = null
) {

    FastScrollerHelper.applyTo(recyclerView)
    swipeRefresh.configure {
        viewModel.refresh(true)
    }

    observe(viewModel.userFlow) {
        viewModel.refresh(true)
    }

    viewModel.initialize()
    var shelfAdapter: ShelfAdapter? = null
    var job: Job? = null
    if (clientId == null) collect(viewModel.extensionFlow) {
        job?.cancel()
        val adapter = applyClient<T>(recyclerView, swipeRefresh, id, it)
        job = adapter?.applyCurrent(this, recyclerView)
        shelfAdapter = adapter
    }
    else collect(viewModel.extensionListFlow) {
        job?.cancel()
        val extension = viewModel.extensionListFlow.getExtension(clientId)
        val adapter = applyClient<T>(recyclerView, swipeRefresh, id, extension)
        job = adapter?.applyCurrent(this, recyclerView)
        shelfAdapter = adapter
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

    observe(viewModel.loading) {
        tabListener.enabled = !it
        swipeRefresh.isRefreshing = it
    }

    tabLayout.addOnTabSelectedListener(tabListener)
    observe(viewModel.genres) { genres ->
        tabLayout.removeAllTabs()
        tabListener.tabs = genres
        tabLayout.isVisible = genres.isNotEmpty()
        genres.forEach { genre ->
            val tab = tabLayout.newTab()
            tab.text = genre.title
            val selected = viewModel.tab?.id == genre.id
            tabLayout.addTab(tab, selected)
        }
    }

    observe(viewModel.feed) {
        shelfAdapter?.submit(it)
    }
}