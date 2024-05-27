package dev.brahmkshatriya.echo.ui.extension

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.tabs.TabLayout
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentManageExtensionsBinding
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.utils.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.setupTransition
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsetsMain
import kotlinx.coroutines.flow.MutableStateFlow

class ManageExtensionsFragment : Fragment() {
    var binding by autoCleared<FragmentManageExtensionsBinding>()
    val viewModel by activityViewModels<ExtensionViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentManageExtensionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsetsMain(binding.appBarLayout, binding.recyclerView)
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.appBarOutline.alpha = offset
            binding.appBarOutline.isVisible = offset > 0
            binding.toolBar.alpha = 1 - offset
        }
        binding.toolBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        FastScrollerHelper.applyTo(binding.recyclerView)
        val refresh = binding.toolBar.findViewById<View>(R.id.menu_refresh)
        refresh.setOnClickListener { viewModel.refresh() }
        binding.swipeRefresh.setProgressViewOffset(true, 0, 32.dpToPx(requireContext()))
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        val flow = MutableStateFlow(
            viewModel.extensionListFlow.value?.map { it.metadata }
        )
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> flow.value = viewModel.extensionListFlow.value?.map { it.metadata }
                    1 -> flow.value = viewModel.trackerListFlow.value?.map { it.metadata }
                    2 -> flow.value = viewModel.lyricsListFlow.value?.map { it.metadata }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        val extensionAdapter = ExtensionAdapter { metadata, view1 ->
            openFragment(
                ExtensionInfoFragment.newInstance(metadata, binding.tabLayout.selectedTabPosition),
                view1
            )
        }
        binding.recyclerView.adapter = extensionAdapter.withEmptyAdapter()
        observe(flow) { extensionAdapter.submit(it ?: emptyList()) }
        observe(viewModel.refresher){
            binding.swipeRefresh.isRefreshing = it
        }
    }
}