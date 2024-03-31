package dev.brahmkshatriya.echo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.databinding.OldFragmentRecyclerBinding
import dev.brahmkshatriya.echo.player.ui.PlayerBackButtonHelper
import dev.brahmkshatriya.echo.ui.adapters.HeaderAdapter
import dev.brahmkshatriya.echo.ui.adapters.MediaItemsContainerAdapter
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.getAdapterForExtension

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var binding: OldFragmentRecyclerBinding by autoCleared()
    private val homeViewModel: HomeViewModel by activityViewModels()

    private val headerAdapter = HeaderAdapter(R.string.home) { a, it ->
        homeViewModel.setGenre(it)
        a.submitChips(homeViewModel.getGenres())
        mediaItemsContainerAdapter.refresh()
    }
    private val mediaItemsContainerAdapter =
        MediaItemsContainerAdapter(this)
    private val concat = mediaItemsContainerAdapter.withLoadingFooter()
    private val concatAdapter = ConcatAdapter(headerAdapter, concat)


    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = OldFragmentRecyclerBinding.inflate(inflater, parent, false)
//        enterTransition = MaterialFade()
//        exitTransition = MaterialFade()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        PlayerBackButtonHelper.addCallback(this) {
//            binding.recyclerView.updatePaddingWithPlayerAndSystemInsets(it)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.statusBarScrim) { _, insets ->
            val i = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.statusBarScrim.updateLayoutParams { height = i.top }
            insets
        }

        postponeEnterTransition()
        binding.recyclerView.doOnPreDraw {
            startPostponedEnterTransition()
        }

        binding.swipeRefresh.setProgressViewOffset(true, 0, 72.dpToPx())

        binding.swipeRefresh.setOnRefreshListener {
            homeViewModel.loadGenres()
            mediaItemsContainerAdapter.refresh()
        }
        mediaItemsContainerAdapter.addLoadStateListener {
            binding.swipeRefresh.isRefreshing = it.refresh is LoadState.Loading
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        observe(homeViewModel.homeFeedFlow.flow) {
            binding.recyclerView.adapter = getAdapterForExtension<HomeFeedClient>(
                it, R.string.home, concatAdapter
            ) { client ->
                binding.swipeRefresh.isEnabled = client != null
            }
        }
        observe(homeViewModel.feed) {
            if (it != null) mediaItemsContainerAdapter.submit(it)
        }
        observe(homeViewModel.genres) {
            headerAdapter.submitChips(homeViewModel.getGenres())
        }
    }
}