package dev.brahmkshatriya.echo.ui.feed

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.FragmentGenericCollapsableBinding
import dev.brahmkshatriya.echo.databinding.FragmentRecyclerWithRefreshBinding
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.extensions.cache.Cached
import dev.brahmkshatriya.echo.extensions.cache.Cached.savingFeed
import dev.brahmkshatriya.echo.ui.common.GridAdapter.Companion.configureGridLayout
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.configure
import dev.brahmkshatriya.echo.ui.extensions.login.LoginFragment.Companion.bind
import dev.brahmkshatriya.echo.ui.feed.FeedAdapter.Companion.getFeedAdapter
import dev.brahmkshatriya.echo.ui.feed.FeedAdapter.Companion.getTouchHelper
import dev.brahmkshatriya.echo.ui.feed.FeedClickListener.Companion.getFeedListener
import dev.brahmkshatriya.echo.ui.main.MainFragment.Companion.applyPlayerBg
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper
import kotlinx.coroutines.flow.combine
import org.koin.androidx.viewmodel.ext.android.viewModel

class FeedFragment : Fragment(R.layout.fragment_generic_collapsable) {
    companion object {
        fun getBundle(title: String, subtitle: String?) = Bundle().apply {
            putString("title", title)
            putString("subtitle", subtitle)
        }
    }

    class VM : ViewModel() {
        var initialized = false
        lateinit var extensionId: String
        lateinit var feedId: String
        lateinit var feed: Feed<Shelf>
    }

    private val activityVm by activityViewModels<VM>()
    private val vm by viewModels<VM>()

    private val feedData by lazy {
        val feedViewModel by viewModel<FeedViewModel>()
        if (!vm.initialized) {
            vm.initialized = true
            vm.extensionId = activityVm.extensionId
            vm.feedId = activityVm.feedId
            vm.feed = activityVm.feed
        }
        feedViewModel.getFeedData(
            vm.feedId,
            cached = {
                val feed = Cached.getFeedShelf(app, vm.extensionId, vm.feedId)
                FeedData.State(vm.extensionId, null, feed.getOrThrow())
            }
        ) {
            val extension = music.getExtensionOrThrow(vm.extensionId)
            val feed = savingFeed(app, extension, vm.feedId, vm.feed)
            FeedData.State(extension.id, null, feed)
        }
    }

    private val title by lazy { arguments?.getString("title")!! }
    private val subtitle by lazy { arguments?.getString("subtitle") }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentGenericCollapsableBinding.bind(view)
        binding.bind(this, false)
        binding.extensionIcon.isVisible = false
        binding.toolBar.title = title
        binding.toolBar.subtitle = subtitle
        applyPlayerBg(view) {
            mainBgDrawable.combine(feedData.backgroundImageFlow) { a, b -> b ?: a }
        }
        if (savedInstanceState == null) childFragmentManager.commit {
            replace<Actual>(R.id.genericFragmentContainer, null, arguments)
        }
    }

    class Actual() : Fragment(R.layout.fragment_recycler_with_refresh) {
        private val feedData by lazy {
            val vm by requireParentFragment().viewModel<FeedViewModel>()
            vm.feedDataMap.values.first()
        }

        private val listener by lazy { getFeedListener(requireParentFragment()) }
        private val feedAdapter by lazy {
            getFeedAdapter(feedData, listener)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            val binding = FragmentRecyclerWithRefreshBinding.bind(view)
            applyInsets {
                binding.recyclerView.applyContentInsets(it, 20, 8, 16)
            }
            FastScrollerHelper.applyTo(binding.recyclerView)
            configureGridLayout(
                binding.recyclerView,
                feedAdapter.withLoading(this)
            )
            getTouchHelper(listener).attachToRecyclerView(binding.recyclerView)
            binding.swipeRefresh.run {
                configure()
                setOnRefreshListener { feedData.refresh() }
                observe(feedData.isRefreshingFlow) {
                    isRefreshing = it
                }
            }
        }
    }
}