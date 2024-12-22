package dev.brahmkshatriya.echo.ui.download

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.paging.PagingData
import androidx.recyclerview.widget.ConcatAdapter
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.FragmentDownloadingBinding
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.offline.OfflineExtension
import dev.brahmkshatriya.echo.ui.adapter.ShelfAdapter
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.item.ItemFragment
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.setupTransition
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

        val adapter = DownloadingAdapter(object : DownloadingAdapter.Listener {
            override fun onDownloadItemClick(download: DownloadItem.Single) {
                requireActivity().openFragment(
                    ItemFragment.newInstance(download.clientId, download.item)
                )
            }

            override fun onGroupToggled(download: DownloadItem.Group, checked: Boolean) {
                viewModel.toggleGroup(download.name, checked)
            }

            override fun onDownloadingToggled(
                download: DownloadItem.Single, isDownloading: Boolean
            ) {
                viewModel.toggleDownloading(download, isDownloading)
            }

            override fun onDownloadRemove(download: DownloadItem.Single) {
                viewModel.removeDownload(download)
            }
        })

        val offline = viewModel.extensionListFlow.getExtension(OfflineExtension.metadata.id)
            ?: return
        val downloadedAdapter = ShelfAdapter(this, "downloads", offline)
        downloadedAdapter.applyCurrent(this, binding.recyclerView)

        val titleAdapter = ShelfAdapter(this, "", offline)

        val concatAdapter = ConcatAdapter(
            adapter.withEmptyAdapter(),
            titleAdapter,
            downloadedAdapter.withLoaders()
        )

        binding.recyclerView.adapter = concatAdapter

        observe(viewModel.offlineFlow) {
            titleAdapter.submit(
                PagingData.from(
                    listOf(Shelf.Category(getString(R.string.downloads), null))
                )
            )
            downloadedAdapter.submit(it)
        }
        observe(viewModel.downloads) { adapter.submit(it) }
    }
}