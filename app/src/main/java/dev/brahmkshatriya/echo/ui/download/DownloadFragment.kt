package dev.brahmkshatriya.echo.ui.download

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.paging.LoadState
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.databinding.FragmentDownloadBinding
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension.Companion.getFeed
import dev.brahmkshatriya.echo.ui.common.ExceptionFragment
import dev.brahmkshatriya.echo.ui.common.ExceptionUtils
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.common.GridAdapter
import dev.brahmkshatriya.echo.ui.common.GridAdapter.Companion.configureGridLayout
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyFabInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.download.DownloadsAdapter.Companion.toItems
import dev.brahmkshatriya.echo.ui.feed.FeedAdapter.Companion.getFeedAdapter
import dev.brahmkshatriya.echo.ui.feed.FeedAdapter.Companion.getTouchHelper
import dev.brahmkshatriya.echo.ui.feed.FeedClickListener.Companion.getFeedListener
import dev.brahmkshatriya.echo.ui.feed.FeedData
import dev.brahmkshatriya.echo.ui.feed.FeedViewModel
import dev.brahmkshatriya.echo.ui.media.LineAdapter
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.ui.UiUtils.configureAppBar
import org.koin.androidx.viewmodel.ext.android.viewModel

class DownloadFragment : Fragment(R.layout.fragment_download) {

    private val vm by viewModel<DownloadViewModel>()
    private val downloadsAdapter by lazy {
        DownloadsAdapter(object : DownloadsAdapter.Listener {
            override fun onCancel(trackId: Long) = vm.cancel(trackId)
            override fun onRestart(trackId: Long) = vm.restart(trackId)
            override fun onExceptionClicked(data: ExceptionUtils.Data) = requireActivity()
                .openFragment<ExceptionFragment>(null, ExceptionFragment.getBundle(data))
        })
    }

    private val feedViewModel by viewModel<FeedViewModel>()
    private val feedData by lazy {
        val flow = vm.downloader.unified.downloadFeed
        feedViewModel.getFeedData(
            "downloads", Feed.Buttons(), false, flow
        ) {
            val feed = requireContext().getFeed(flow.value)
            FeedData.State(UnifiedExtension.metadata.id, null, feed)
        }
    }

    private val feedListener by lazy { getFeedListener() }
    private val feedAdapter by lazy {
        getFeedAdapter(feedData, feedListener)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentDownloadBinding.bind(view)
        setupTransition(view)
        applyBackPressCallback()
        binding.appBarLayout.configureAppBar { offset ->
            binding.toolbarOutline.alpha = offset
            binding.iconContainer.alpha = 1 - offset
        }
        binding.toolBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        applyInsets {
            binding.recyclerView.applyContentInsets(it, 20, 8, 72)
            binding.fabContainer.applyFabInsets(it, systemInsets.value)
        }
        FastScrollerHelper.applyTo(binding.recyclerView)
        val lineAdapter = LineAdapter()
        binding.fabCancel.setOnClickListener {
            vm.cancelAll()
        }
        binding.recyclerView.itemAnimator = null
        getTouchHelper(feedListener).attachToRecyclerView(binding.recyclerView)
        configureGridLayout(
            binding.recyclerView,
            GridAdapter.Concat(
                downloadsAdapter,
                lineAdapter,
                feedAdapter.withLoading(this)
            )
        )
        observe(vm.flow) { infos ->
            binding.fabCancel.isVisible = infos.any { it.download.finalFile == null }
            lineAdapter.loadState = if (infos.isNotEmpty()) LoadState.Loading
            else LoadState.NotLoading(false)
            downloadsAdapter.submitList(infos.toItems(vm.extensions.music.value))
        }
    }
}