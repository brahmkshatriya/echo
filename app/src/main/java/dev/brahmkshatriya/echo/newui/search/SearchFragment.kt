package dev.brahmkshatriya.echo.newui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.tabs.TabLayout
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.databinding.FragmentHomeBinding
import dev.brahmkshatriya.echo.newui.MediaContainerAdapter
import dev.brahmkshatriya.echo.newui.getAdapterForExtension
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe

class SearchFragment : Fragment() {
    private var binding by autoCleared<FragmentHomeBinding>()
    private val viewModel by activityViewModels<SearchViewModel>()

    private val mediaContainerAdapter = MediaContainerAdapter(this)
    private val concatAdapter = mediaContainerAdapter.withLoaders()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.appBarLayout.addOnOffsetChangedListener { appbar, verticalOffset ->
            val offset = (-verticalOffset) / appbar.totalScrollRange.toFloat()
            binding.appBarOutline.alpha = offset
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh(true)
        }
//        binding.quickSearchView.setupWithSearchBar(binding.searchBar)
        binding.quickSearchView.editText.doOnTextChanged { text, _, _, _ ->
            println("text : $text")
        }
        binding.quickSearchView.editText.setOnEditorActionListener { textView, _, _ ->
            val query = textView.text.toString()
            viewModel.query = query
//            binding.searchBar.setText(query)
            binding.quickSearchView.hide()
            viewModel.refresh()
            false
        }
        viewModel.initialize()

        observe(viewModel.extensionFlow.flow) {
            binding.swipeRefresh.isEnabled = it != null
            binding.recyclerView.adapter =
                getAdapterForExtension<SearchClient>(it, R.string.search, concatAdapter)
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

        observe(viewModel.searchFeed) {
            mediaContainerAdapter.submit(it)
        }
    }

}