package dev.brahmkshatriya.echo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.tabs.TabLayout
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.databinding.FragmentHomeBinding
import dev.brahmkshatriya.echo.ui.common.MainFragment
import dev.brahmkshatriya.echo.ui.common.MainFragment.Companion.first
import dev.brahmkshatriya.echo.ui.common.MainFragment.Companion.scrollTo
import dev.brahmkshatriya.echo.ui.common.configureMainMenu
import dev.brahmkshatriya.echo.ui.media.MediaContainerAdapter
import dev.brahmkshatriya.echo.ui.media.MediaContainerLoadingAdapter.Companion.withLoaders
import dev.brahmkshatriya.echo.utils.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.collect
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.setupTransition
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.applyAdapter
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsetsMain

class HomeFragment : Fragment() {

    private var binding by autoCleared<FragmentHomeBinding>()
    private val viewModel by activityViewModels<HomeViewModel>()
    private val parent get() = parentFragment as MainFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsetsMain(binding.appBarLayout, binding.recyclerView)
        applyBackPressCallback()
        binding.toolBar.configureMainMenu(parent)
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.appBarOutline.alpha = offset
            binding.appBarOutline.isVisible = offset > 0
            binding.toolBar.alpha = 1 - offset
        }
        FastScrollerHelper.applyTo(binding.recyclerView)
        binding.swipeRefresh.setProgressViewOffset(true, 0, 32.dpToPx(requireContext()))
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh(true)
        }

        observe(viewModel.userFlow) {
            viewModel.refresh(true)
        }

        viewModel.initialize()
        val mediaContainerAdapter = MediaContainerAdapter(parent, "home")
        val concatAdapter = mediaContainerAdapter.withLoaders()
        collect(viewModel.extensionFlow) {
            binding.swipeRefresh.isEnabled = it != null
            mediaContainerAdapter.clientId = it?.metadata?.id
            binding.recyclerView.applyAdapter<HomeFeedClient>(it, R.string.home, concatAdapter)
        }

        val tabListener = object : TabLayout.OnTabSelectedListener {
            var enabled = true
            var genres: List<Genre> = emptyList()
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (!enabled) return
                viewModel.genre = genres[tab.position]
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        }

        observe(viewModel.loading) {
            tabListener.enabled = !it
            binding.swipeRefresh.isRefreshing = it
        }

        binding.tabLayout.addOnTabSelectedListener(tabListener)
        observe(viewModel.genres) { genres ->
            binding.tabLayout.removeAllTabs()
            tabListener.genres = genres
            binding.tabLayout.isVisible = genres.isNotEmpty()
            genres.forEach { genre ->
                val tab = binding.tabLayout.newTab()
                tab.text = genre.name
                val selected = viewModel.genre == genre
                binding.tabLayout.addTab(tab, selected)
            }
        }

        observe(viewModel.homeFeed) {
            mediaContainerAdapter.submit(it)
        }

        binding.recyclerView.scrollTo(viewModel.recyclerPosition) {
            binding.appBarLayout.setExpanded(it < 1, false)
        }
        view.doOnLayout {
            binding.appBarOutline.alpha = 0f
        }
    }

    override fun onStop() {
        viewModel.recyclerPosition = binding.recyclerView.first()
        super.onStop()
    }
}