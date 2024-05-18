package dev.brahmkshatriya.echo.ui.download

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentDownloadingBinding
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
            binding.exceptionIconContainer.updatePadding(top = it.top)
            binding.recyclerView.applyContentInsets(it)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
        }
        binding.toolBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        val downloads = binding.toolBar.findViewById<View>(R.id.menu_downloads)
        downloads.transitionName = "downloads"
        downloads.setOnClickListener {
            viewModel.loadOfflineDownloads()
            openFragment(DownloadFragment(), downloads)
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

        binding.recyclerView.adapter = adapter.withEmptyAdapter()
        observe(viewModel.downloads){ adapter.submit(it) }
    }
}