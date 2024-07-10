package dev.brahmkshatriya.echo.ui.common

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.ui.media.MediaContainerAdapter
import dev.brahmkshatriya.echo.utils.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.collect
import dev.brahmkshatriya.echo.utils.configure
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.applyAdapter

fun Fragment.configureFeedUI(
    id: Int,
    viewModel: FeedViewModel,
    recyclerView: RecyclerView,
    swipeRefresh: SwipeRefreshLayout,
    tabLayout: TabLayout,
    listener: MediaContainerAdapter.Listener? = null
): MediaContainerAdapter {

    val parent = parentFragment as Fragment

    FastScrollerHelper.applyTo(recyclerView)
    swipeRefresh.configure {
        viewModel.refresh(true)
    }
    observe(viewModel.userFlow) {
        viewModel.refresh(true)
    }

    viewModel.initialize()
    val mediaContainerAdapter = if (listener == null)
        MediaContainerAdapter(parent, id.toString())
    else MediaContainerAdapter(parent, id.toString(), listener)

    if (listener == null) {
        val concatAdapter = mediaContainerAdapter.withLoaders()
        collect(viewModel.extensionFlow) {
            swipeRefresh.isEnabled = it != null
            mediaContainerAdapter.clientId = it?.metadata?.id
            recyclerView.applyAdapter<HomeFeedClient>(it, id, concatAdapter)
        }
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
            tab.text = genre.name
            val selected = viewModel.tab?.id == genre.id
            tabLayout.addTab(tab, selected)
        }
    }

    observe(viewModel.feed) {
        println("Feed : $it")
        mediaContainerAdapter.submit(it)
    }

    return mediaContainerAdapter
}