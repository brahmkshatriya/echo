package dev.brahmkshatriya.echo.ui.download

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toResourceImageHolder
import dev.brahmkshatriya.echo.databinding.FragmentDownloadBinding
import dev.brahmkshatriya.echo.ui.common.ExceptionFragment
import dev.brahmkshatriya.echo.ui.common.ExceptionUtils
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyFabInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.download.DownloadsAdapter.Companion.toItems
import dev.brahmkshatriya.echo.ui.main.settings.BaseSettingsFragment
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfAdapter.Companion.getShelfAdapter
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfClickListener.Companion.getShelfListener
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.androidx.viewmodel.ext.android.viewModel

class DownloadFragment : Fragment() {

    private var binding by autoCleared<FragmentDownloadBinding>()
    private val vm by viewModel<DownloadViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val downloadsAdapter by lazy {
        DownloadsAdapter(object : DownloadsAdapter.Listener {
            override fun onExceptionClicked(data: ExceptionUtils.Data) {
                requireActivity()
                    .openFragment<ExceptionFragment>(null, ExceptionFragment.getBundle(data))
            }

            override fun onCancel(trackId: Long) {
                vm.cancel(trackId)
            }

            override fun onRestart(trackId: Long) {
                vm.restart(trackId)
            }
        })
    }

    private val shelfAdapter by lazy { getShelfAdapter(getShelfListener()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        applyInsets {
            binding.recyclerView.applyContentInsets(it)
            binding.fabContainer.applyFabInsets(it, systemInsets.value)
        }
        binding.fabCancel.setOnClickListener {
            vm.cancelAll()
        }
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = ConcatAdapter(
            downloadsAdapter,
            shelfAdapter.withHeaders(this, vm, vm.downloaded, MutableStateFlow(null))
        )
        observe(vm.flow) { infos ->
            binding.fabCancel.isVisible = infos.any { it.download.finalFile == null }
            downloadsAdapter.submitList(infos.toItems(vm.extensions.music.value))
        }
        observe(vm.downloaded) { (ext, _, data, page) ->
            shelfAdapter.submit(ext?.id, data, page)
        }
        shelfAdapter.getTouchHelper().attachToRecyclerView(binding.recyclerView)
    }

    class WithHeader : BaseSettingsFragment() {
        override val title: String
            get() = getString(R.string.downloads)
        override val icon = R.drawable.ic_downloading.toResourceImageHolder()
        override val creator = { DownloadFragment() }
    }
}