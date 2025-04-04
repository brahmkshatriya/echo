package dev.brahmkshatriya.echo.ui.download

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import dev.brahmkshatriya.echo.databinding.FragmentDownloadBinding
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyFabInsets
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.common.ExceptionFragment
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.download.DownloadsAdapter.Companion.toItems
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfAdapter.Companion.getShelfAdapter
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfClickListener.Companion.getShelfListener
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ExceptionUtils
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.UiUtils.onAppBarChangeListener
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
                openFragment<ExceptionFragment>(null, ExceptionFragment.getBundle(data))
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
        setupTransition(view)
        applyInsets {
            binding.recyclerView.applyContentInsets(it)
            binding.fabContainer.applyFabInsets(it, systemInsets.value)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
        }

        binding.exceptionMessage.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.fabCancel.setOnClickListener {
            vm.cancelAll()
        }
        binding.recyclerView.adapter = ConcatAdapter(
            downloadsAdapter,
            shelfAdapter.withHeaders(this, vm, vm.downloaded, MutableStateFlow(null))
        )
        observe(vm.flow) { infos ->
            binding.fabCancel.isVisible = infos.any { it.download.finalFile == null }
            downloadsAdapter.submitList(infos.toItems(vm.extensions.music.value.orEmpty()))
        }
        observe(vm.downloaded) { (ext, _, data, page) ->
            shelfAdapter.submit(ext?.id, data, page)
        }
        shelfAdapter.getTouchHelper().attachToRecyclerView(binding.recyclerView)
    }
}