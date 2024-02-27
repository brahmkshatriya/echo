package dev.brahmkshatriya.echo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.databinding.FragmentRecyclerBinding
import dev.brahmkshatriya.echo.player.ui.PlayerBackButtonHelper
import dev.brahmkshatriya.echo.ui.MediaItemClickListener
import dev.brahmkshatriya.echo.ui.adapters.HeaderAdapter
import dev.brahmkshatriya.echo.ui.adapters.MediaItemsContainerAdapter
import dev.brahmkshatriya.echo.ui.extension.getAdapterForExtension
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.updatePaddingWithSystemInsets

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var binding: FragmentRecyclerBinding by autoCleared()
    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = FragmentRecyclerBinding.inflate(inflater, parent, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        PlayerBackButtonHelper.addCallback(this)
        updatePaddingWithSystemInsets(binding.recyclerView)
        binding.swipeRefresh.setProgressViewOffset(true, 0, 72.dpToPx())

        val headerAdapter = HeaderAdapter(R.string.home)
        val mediaItemsContainerAdapter = MediaItemsContainerAdapter(
            lifecycle, MediaItemClickListener(this)
        )

        val concat = mediaItemsContainerAdapter.withLoadingFooter()
        binding.swipeRefresh.setOnRefreshListener {
            mediaItemsContainerAdapter.refresh()
        }
        mediaItemsContainerAdapter.addLoadStateListener {
            binding.swipeRefresh.isRefreshing = it.refresh is LoadState.Loading
        }

        val concatAdapter = ConcatAdapter(headerAdapter, concat)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        observe(homeViewModel.homeFeedFlow.flow) {
            binding.recyclerView.adapter = getAdapterForExtension<HomeFeedClient>(
                it,
                R.string.home,
                concatAdapter
            ) { client ->
                binding.swipeRefresh.isEnabled = client != null
            }
        }

        observe(homeViewModel.feed) {
            if (it != null) mediaItemsContainerAdapter.submitData(it)
        }
    }
}