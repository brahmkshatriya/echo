package dev.brahmkshatriya.echo.ui.download

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.brahmkshatriya.echo.databinding.FragmentDownloadBinding
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyFabInsets
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.download.DownloadsAdapter.Companion.toItems
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.UiUtils.onAppBarChangeListener
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class DownloadFragment : Fragment() {

    private var binding by autoCleared<FragmentDownloadBinding>()
    private val vm by activityViewModel<DownloadViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val adapter by lazy { DownloadsAdapter() }

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

        binding.recyclerView.adapter = adapter
        observe(vm.flow) {
            println("buhr ${it.size}")
            println(it)
            adapter.submitList(it.toItems().also { println("item ${it.size}") })
        }
    }
}