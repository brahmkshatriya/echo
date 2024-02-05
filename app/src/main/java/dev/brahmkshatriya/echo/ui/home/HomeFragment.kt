package dev.brahmkshatriya.echo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentRecyclerBinding
import dev.brahmkshatriya.echo.ui.adapters.HeaderAdapter
import dev.brahmkshatriya.echo.ui.adapters.MediaItemsContainerAdapter
import dev.brahmkshatriya.echo.ui.adapters.ContainerLoadingAdapter
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.utils.autoCleared
import dev.brahmkshatriya.echo.ui.utils.dpToPx
import dev.brahmkshatriya.echo.ui.utils.observeFlow
import dev.brahmkshatriya.echo.ui.utils.updatePaddingWithSystemInsets

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var binding: FragmentRecyclerBinding by autoCleared()
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = FragmentRecyclerBinding.inflate(inflater, parent, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val playerViewModel by activityViewModels<PlayerViewModel>()
        playerViewModel.handleBackPress(this)
        updatePaddingWithSystemInsets(binding.recyclerView)
        binding.swipeRefresh.setProgressViewOffset(true, 0, 72.dpToPx())

        val headerAdapter = HeaderAdapter(R.string.home)
        val mediaItemsContainerAdapter =
            MediaItemsContainerAdapter(viewLifecycleOwner.lifecycle, playerViewModel::play)

        mediaItemsContainerAdapter.withLoadStateFooter(ContainerLoadingAdapter{
            homeViewModel.loadFeed(homeViewModel.genre)
        })

        binding.recyclerView.adapter = ConcatAdapter(headerAdapter, mediaItemsContainerAdapter)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        binding.swipeRefresh.setOnRefreshListener {
            homeViewModel.loadFeed(homeViewModel.genre)
        }

        homeViewModel.feed.observeFlow(viewLifecycleOwner) {
            binding.swipeRefresh.isRefreshing = false
            if (it == null) return@observeFlow
            mediaItemsContainerAdapter.submitData(it)
        }

        homeViewModel.loadFeed(homeViewModel.genre)
    }
}