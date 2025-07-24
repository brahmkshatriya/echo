package dev.brahmkshatriya.echo.ui.main.search

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.transition.MaterialSharedAxis
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.SearchFeedClient
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Buttons.Companion.EMPTY
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.FragmentSearchBinding
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getAs
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.ui.common.GridAdapter.Companion.configureGridLayout
import dev.brahmkshatriya.echo.ui.common.UiViewModel
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.configure
import dev.brahmkshatriya.echo.ui.feed.FeedAdapter.Companion.getFeedAdapter
import dev.brahmkshatriya.echo.ui.feed.FeedAdapter.Companion.getTouchHelper
import dev.brahmkshatriya.echo.ui.feed.FeedClickListener.Companion.getFeedListener
import dev.brahmkshatriya.echo.ui.feed.FeedData
import dev.brahmkshatriya.echo.ui.feed.FeedViewModel
import dev.brahmkshatriya.echo.ui.main.HeaderAdapter
import dev.brahmkshatriya.echo.ui.main.MainFragment.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.main.search.SearchViewModel.Companion.saveInHistory
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import kotlinx.coroutines.flow.combine
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchFragment : Fragment(R.layout.fragment_search) {

    private val argId by lazy { arguments?.getString("extensionId") }
    private val searchViewModel by viewModel<SearchViewModel>()

    private var extensionId = ""

    private val feedData by lazy {
        val vm by viewModel<FeedViewModel>()
        vm.getFeedData("search", EMPTY, false, searchViewModel.queryFlow) {
            val curr = music.getExtension(argId) ?: current.value!!
            val query = searchViewModel.queryFlow.value
            curr.saveInHistory(requireContext(), query)
            val feed = curr.getAs<SearchFeedClient, Feed<Shelf>> {
                loadSearchFeed(searchViewModel.queryFlow.value)
            }.getOrThrow()
            extensionId = curr.id
            FeedData.State(curr.id, null, feed)
        }
    }

    private val listener by lazy {
        getFeedListener(if (argId == null) requireParentFragment() else this)
    }

    private val feedAdapter by lazy {
        getFeedAdapter(feedData, listener)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentSearchBinding.bind(view)
        setupTransition(view, false, MaterialSharedAxis.Y)
        applyInsets(binding.recyclerView, binding.appBarOutline) {
            binding.swipeRefresh.configure(it)
        }
        val uiViewModel by activityViewModel<UiViewModel>()
        observe(uiViewModel.navigationReselected) {
            if (it != 1) return@observe
            binding.quickSearchView.show()
        }
        observe(uiViewModel.navigation) {
            binding.quickSearchView.hide()
        }
        observe(
            uiViewModel.navigation.combine(feedData.backgroundImageFlow) { a, b -> a to b }
        ) { (curr, bg) ->
            if (curr != 1) return@observe
            uiViewModel.currentNavBackground.value = bg
        }
        val backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                binding.quickSearchView.hide()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
        binding.quickSearchView.addTransitionListener { v, _, _ ->
            backCallback.isEnabled = v.isShowing
        }
        applyBackPressCallback {
            if (it == STATE_EXPANDED) binding.quickSearchView.hide()
        }
        val searchAdapter = SearchBarAdapter(searchViewModel, binding.quickSearchView)
        observe(searchViewModel.queryFlow) {
            searchAdapter.notifyItemChanged(0)
            binding.quickSearchView.setText(it)
        }
        getTouchHelper(listener).attachToRecyclerView(binding.recyclerView)
        configureGridLayout(
            binding.recyclerView,
            feedAdapter.withLoading(this, HeaderAdapter(this), searchAdapter)
        )
        binding.swipeRefresh.run {
            setOnRefreshListener { feedData.refresh() }
            observe(feedData.isRefreshingFlow) {
                isRefreshing = it
            }
        }
        binding.quickSearchView.editText.setText(searchViewModel.queryFlow.value)
        binding.quickSearchView.editText.doOnTextChanged { text, _, _, _ ->
            searchViewModel.quickSearch(extensionId, text.toString())
        }
        binding.quickSearchView.editText.setOnEditorActionListener { textView, _, _ ->
            val query = textView.text.toString()
            binding.quickSearchView.hide()
            searchViewModel.queryFlow.value = query
            false
        }
        val quickSearchAdapter = QuickSearchAdapter(object : QuickSearchAdapter.Listener {
            override fun onClick(item: QuickSearchAdapter.Item, transitionView: View) {
                when (val actualItem = item.actual) {
                    is QuickSearchItem.Query -> {
                        binding.quickSearchView.editText.run {
                            setText(actualItem.query)
                            onEditorAction(imeOptions)
                        }
                    }

                    is QuickSearchItem.Media -> {
                        val extensionId = item.extensionId
                        listener.onMediaClicked(transitionView, extensionId, actualItem.media, null)
                    }
                }
            }

            override fun onDeleteClick(item: QuickSearchAdapter.Item) =
                searchViewModel.deleteSearch(
                    item.extensionId,
                    item.actual,
                    binding.quickSearchView.editText.text.toString()
                )

            override fun onLongClick(item: QuickSearchAdapter.Item, transitionView: View) =
                when (val actualItem = item.actual) {
                    is QuickSearchItem.Query -> {
                        onDeleteClick(item)
                        true
                    }

                    is QuickSearchItem.Media -> {
                        val extensionId = item.extensionId
                        listener.onMediaLongClicked(
                            transitionView, extensionId, actualItem.media,
                            null, null, -1
                        )
                        true
                    }
                }

            override fun onInsert(item: QuickSearchAdapter.Item) {
                binding.quickSearchView.editText.run {
                    setText(item.actual.title)
                    setSelection(length())
                }
            }
        })

        binding.quickSearchRecyclerView.adapter = quickSearchAdapter
        observe(searchViewModel.quickFeed) { list ->
            quickSearchAdapter.submitList(list.map {
                QuickSearchAdapter.Item(extensionId, it)
            })
        }
    }
}