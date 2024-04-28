package dev.brahmkshatriya.echo.ui.search

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.tabs.TabLayout
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.models.Genre
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.databinding.FragmentSearchBinding
import dev.brahmkshatriya.echo.ui.common.MainFragment
import dev.brahmkshatriya.echo.ui.common.MainFragment.Companion.first
import dev.brahmkshatriya.echo.ui.common.MainFragment.Companion.scrollTo
import dev.brahmkshatriya.echo.ui.common.configureMainMenu
import dev.brahmkshatriya.echo.ui.media.MediaClickListener
import dev.brahmkshatriya.echo.ui.media.MediaContainerAdapter
import dev.brahmkshatriya.echo.utils.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.collect
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.setupTransition
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.applyAdapter
import dev.brahmkshatriya.echo.viewmodels.UiViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsetsMain

class SearchFragment : Fragment() {

    private var binding by autoCleared<FragmentSearchBinding>()
    private val viewModel by activityViewModels<SearchViewModel>()
    private val uiViewModel by activityViewModels<UiViewModel>()

    private val parent get() = parentFragment as MainFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsetsMain(binding.appBarLayout, binding.recyclerView) {
            binding.quickSearchView.updatePaddingRelative(start = it.start, end = it.end)
        }
        applyBackPressCallback {
            if (it == STATE_EXPANDED) binding.quickSearchView.hide()
        }
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.appBarOutline.alpha = offset
            binding.appBarOutline.isVisible = offset > 0
            binding.searchBar.alpha = 1 - offset
            binding.toolBar.alpha = 1 - offset
        }

        binding.toolBar.configureMainMenu(parent)
        FastScrollerHelper.applyTo(binding.recyclerView)

        binding.swipeRefresh.setProgressViewOffset(true, 0, 32.dpToPx(requireContext()))
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
            viewModel.refresh(true)
            false
        }

        observe(uiViewModel.navigationReselected) {
            if (it == 1) binding.quickSearchView.show()
        }

        viewModel.initialize()

        val mediaClickListener = MediaClickListener(parent.parentFragmentManager)
        val quickSearchAdapter = QuickSearchAdapter(object : QuickSearchAdapter.Listener {
            override fun onClick(item: QuickSearchItem, transitionView: View) = when (item) {
                is QuickSearchItem.SearchQueryItem -> {
                    binding.quickSearchView.editText.run {
                        setText(item.query)
                        onEditorAction(imeOptions)
                    }
                }

                is QuickSearchItem.SearchMediaItem -> {
                    val client = viewModel.extensionFlow.value?.metadata?.id
                    mediaClickListener.onClick(client, item.mediaItem, transitionView)
                }
            }

            override fun onLongClick(item: QuickSearchItem, transitionView: View) = when (item) {
                is QuickSearchItem.SearchQueryItem -> {
                    onClick(item, transitionView)
                    true
                }

                is QuickSearchItem.SearchMediaItem -> {
                    val client = viewModel.extensionFlow.value?.metadata?.id
                    mediaClickListener.onLongClick(client, item.mediaItem, transitionView)
                    true
                }
            }

            override fun onInsert(item: QuickSearchItem) {
                binding.quickSearchView.editText.run {
                    setText(item.title)
                    setSelection(length())
                }
            }
        })

        binding.quickSearchRecyclerView.adapter = quickSearchAdapter

        val mediaContainerAdapter = MediaContainerAdapter(parent, "search", mediaClickListener)
        val concatAdapter = mediaContainerAdapter.withLoaders()

        collect(viewModel.extensionFlow) {
            binding.swipeRefresh.isEnabled = it != null
            mediaContainerAdapter.clientId = it?.metadata?.id
            binding.recyclerView.applyAdapter<SearchClient>(it, R.string.search, concatAdapter)
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

        binding.recyclerView.scrollTo(viewModel.recyclerPosition) {
            binding.appBarLayout.setExpanded(it < 1, false)
        }

        // Need to hide scrim because something weird happens when we use Material Transition
        binding.quickSearchView
            .findViewById<View>(com.google.android.material.R.id.open_search_view_scrim).run {
                setBackgroundColor(Color.TRANSPARENT)
            }
    }

    override fun onStop() {
        viewModel.recyclerPosition = binding.recyclerView.first()
        super.onStop()
    }

}