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
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.databinding.FragmentSearchBinding
import dev.brahmkshatriya.echo.newui.MediaClickListener
import dev.brahmkshatriya.echo.newui.MediaContainerAdapter
import dev.brahmkshatriya.echo.newui.getAdapterForExtension
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe

class SearchFragment : Fragment() {

    private var binding by autoCleared<FragmentSearchBinding>()
    private val viewModel by activityViewModels<SearchViewModel>()

    private val mediaClickListener = MediaClickListener(this)
    private val mediaContainerAdapter = MediaContainerAdapter(this, mediaClickListener)
    private val concatAdapter = mediaContainerAdapter.withLoaders()

    private val quickSearchAdapter = QuickSearchAdapter(object : QuickSearchAdapter.Listener {
        override fun onClick(item: QuickSearchItem, transitionView: View) = when (item) {
            is QuickSearchItem.SearchQueryItem -> {
                binding.quickSearchView.editText.run {
                    setText(item.query)
                    onEditorAction(imeOptions)
                }
            }

            is QuickSearchItem.SearchMediaItem -> {
                mediaClickListener.onClick(item.mediaItem, transitionView)
            }
        }

        override fun onLongClick(item: QuickSearchItem, transitionView: View) = when (item) {
            is QuickSearchItem.SearchQueryItem -> {
                onClick(item, transitionView)
                true
            }

            is QuickSearchItem.SearchMediaItem -> {
                mediaClickListener.onLongClick(item.mediaItem, transitionView)
                true
            }
        }

        override fun onInsert(item: QuickSearchItem) {
            binding.quickSearchView.editText.setText(item.title)
        }
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.appBarLayout.addOnOffsetChangedListener { appbar, verticalOffset ->
            val offset = (-verticalOffset) / appbar.totalScrollRange.toFloat()
            binding.appBarOutline.alpha = offset
            binding.searchBar.alpha = 1 - offset
            binding.toolbar.alpha = 1 - offset
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh(true)
        }
        binding.quickSearchView.setupWithSearchBar(binding.searchBar)
        binding.quickSearchView.editText.doOnTextChanged { text, _, _, _ ->
            viewModel.quickSearch(text.toString())
        }
        binding.quickSearchView.editText.setOnEditorActionListener { textView, _, _ ->
            val query = textView.text.toString()
            viewModel.query = query
            binding.searchBar.setText(query)
            binding.quickSearchView.hide()
            viewModel.refresh()
            false
        }

        binding.quickSearchRecyclerView.adapter = quickSearchAdapter

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

        observe(viewModel.quickFeed) {
            quickSearchAdapter.submitList(it)
        }
    }
}