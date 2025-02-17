package dev.brahmkshatriya.echo.ui.download

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.builtin.offline.OfflineExtension
import dev.brahmkshatriya.echo.databinding.FragmentDownloadingBinding
import dev.brahmkshatriya.echo.ui.adapter.ShelfEmptyAdapter
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.shelf.ShelfFragment
import dev.brahmkshatriya.echo.ui.shelf.ShelfViewModel
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.ui.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.ui.setupTransition
import dev.brahmkshatriya.echo.viewmodels.DownloadViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets

class DownloadingFragment : Fragment() {
    var binding by autoCleared<FragmentDownloadingBinding>()
    val viewModel by activityViewModels<DownloadViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsets {
            binding.recyclerView.applyContentInsets(it)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
        }
        binding.toolBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val downloadsMenu = binding.toolBar.findViewById<View>(R.id.downloads)!!
        downloadsMenu.transitionName = "downloads"
        downloadsMenu.setOnClickListener {
            val vm by activityViewModels<ShelfViewModel>()
            vm.shelves = viewModel.getOfflineDownloads() ?: return@setOnClickListener
            openFragment(
                ShelfFragment.newInstance(
                    OfflineExtension.metadata.id, getString(R.string.downloads)
                ), it
            )
        }

        val downloadAdapter = DownloadItemAdapter(object : DownloadItemAdapter.Listener {
            override fun onCancelClick(taskIds: List<Long>) {
                viewModel.cancelDownload(taskIds)
            }

            override fun onCancelClick(trackId: Long) {
                viewModel.cancelTrackDownload(trackId)
            }

            override fun onPauseClick(taskIds: List<Long>) {
                viewModel.pauseDownload(taskIds)
            }

            override fun onResumeClick(taskIds: List<Long>) {
                viewModel.resumeDownload(taskIds)
            }

            override fun onRetryClick(trackId: Long) {
                viewModel.retryDownload(trackId)
            }

        })

        val emptyAdapter = ShelfEmptyAdapter()
        binding.recyclerView.adapter = ConcatAdapter(
            emptyAdapter,
            downloadAdapter,
        )
        binding.recyclerView.itemAnimator = null

        viewModel.run {
            observe(downloadsFlow) { (tasks, tracks, contexts) ->
                val list = DownloadItem.from(contexts, tracks, tasks, extensionListFlow)
                downloadAdapter.submitList(list) {
                    emptyAdapter.loadState = if (list.isEmpty()) LoadState.Loading
                    else LoadState.NotLoading(false)
                }
            }
        }
    }
}