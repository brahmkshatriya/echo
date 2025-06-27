package dev.brahmkshatriya.echo.ui.shelf

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.brahmkshatriya.echo.databinding.FragmentShelfBinding
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfAdapter.Companion.getShelfAdapter
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfClickListener.Companion.getShelfListener
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoClearedNullable
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.ui.UiUtils.configureAppBar
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ShelfFragment : Fragment() {

    private var binding by autoClearedNullable<FragmentShelfBinding>()
    private val activityVm by activityViewModel<ShelfViewModel>()
    private val vm by viewModel<ShelfViewModel>()

    private val listener by lazy { getShelfListener() }
    private val adapter by lazy { getShelfAdapter(listener) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShelfBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm.initialize(activityVm)
        setupTransition(view)
        applyBackPressCallback()
        val binding = binding!!
        binding.appBarLayout.configureAppBar { offset ->
            binding.toolbarOutline.alpha = offset
        }
        binding.title.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.title.title = vm.title?.trim()
        applyInsets {
            binding.recyclerView.applyContentInsets(it)
        }
        binding.swipeRefresh.setOnRefreshListener { vm.load() }
        FastScrollerHelper.applyTo(binding.recyclerView)
        binding.recyclerView.adapter = adapter.withHeaders(this, vm, vm.feed, vm.job)
        adapter.getTouchHelper().attachToRecyclerView(binding.recyclerView)
        observe(vm.feed) { (ext, _, data, page) ->
            adapter.submit(ext?.id, data, page)
        }
        adapter.addLoadStateListener {
            this.binding?.swipeRefresh?.isRefreshing = false
        }
    }
}