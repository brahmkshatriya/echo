package dev.brahmkshatriya.echo.ui.main.search

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import dev.brahmkshatriya.echo.common.models.QuickSearchItem
import dev.brahmkshatriya.echo.databinding.FragmentSearchBinding
import dev.brahmkshatriya.echo.ui.UiViewModel
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyInsetsMain
import dev.brahmkshatriya.echo.ui.main.FeedViewModel.Companion.configureFeed
import dev.brahmkshatriya.echo.ui.main.MainFragment
import dev.brahmkshatriya.echo.ui.main.MainFragment.Companion.applyPlayerBg
import dev.brahmkshatriya.echo.ui.main.MainFragment.Companion.configureMainMenu
import dev.brahmkshatriya.echo.ui.search.QuickSearchAdapter
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import dev.brahmkshatriya.echo.utils.ui.UiUtils.onAppBarChangeListener
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchFragment : Fragment() {
    private var binding by autoCleared<FragmentSearchBinding>()
    private val uiViewModel by activityViewModel<UiViewModel>()

    private val isMain by lazy { arguments?.getBoolean("isMain", true) ?: true }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val viewModel by requireParentFragment().viewModel<SearchViewModel>()

        setupTransition(view)
        applyPlayerBg(view, binding.appBarLayout)
        applyInsetsMain(binding.appBarLayout, binding.recyclerView) {
            binding.quickSearchView.updatePaddingRelative(start = it.start, end = it.end)
            binding.quickSearchRecyclerView.updatePaddingRelative(bottom = it.bottom)
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
        if (isMain) binding.toolBar.configureMainMenu(parentFragment as MainFragment)
        else {
            binding.toolBar.isVisible = false
            binding.searchBar.updateLayoutParams<MarginLayoutParams> {
                marginEnd = 24.dpToPx(requireContext())
            }
        }

        binding.searchBar.setText(viewModel.query)
        binding.quickSearchView.editText.setText(viewModel.query)

        binding.quickSearchView.setupWithSearchBar(binding.searchBar)
        binding.quickSearchView.editText.doOnTextChanged { text, _, _, _ ->
            viewModel.quickSearch(text.toString())
        }
        binding.quickSearchView.editText.setOnEditorActionListener { textView, _, _ ->
            val query = textView.text.toString()
            binding.searchBar.setText(query)
            binding.quickSearchView.hide()
            if (query != viewModel.query) {
                viewModel.query = query
                viewModel.refresh(true)
            }
            false
        }

        observe(uiViewModel.navigationReselected) {
            if (it == 1) binding.quickSearchView.show()
        }
        val mediaClickListener = configureFeed(
            viewModel, binding.recyclerView, binding.swipeRefresh, binding.tabLayout
        )
        val quickSearchAdapter = QuickSearchAdapter(object : QuickSearchAdapter.Listener {
            override fun onClick(item: QuickSearchItem, transitionView: View) {
                when (item) {
                    is QuickSearchItem.Query -> {
                        binding.quickSearchView.editText.run {
                            setText(item.query)
                            onEditorAction(imeOptions)
                        }
                    }

                    is QuickSearchItem.Media -> {
                        val client = viewModel.current.value?.metadata?.id
                        mediaClickListener.onMediaItemClicked(client, item.media, transitionView)
                    }
                }
            }

            override fun onDeleteClick(item: QuickSearchItem) {
                viewModel.deleteSearch(item, binding.quickSearchView.editText.text.toString())
            }

            override fun onLongClick(item: QuickSearchItem, transitionView: View) = when (item) {
                is QuickSearchItem.Query -> {
                    onDeleteClick(item)
                    true
                }

                is QuickSearchItem.Media -> {
                    val client = viewModel.current.value?.metadata?.id
                    mediaClickListener.onMediaItemLongClicked(client, item.media, transitionView)
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

        observe(viewModel.quickFeed) {
            quickSearchAdapter.submitList(it)
        }

        // Need to hide scrim because something weird shit happens when we use Material Transition
        binding.quickSearchView
            .findViewById<View>(com.google.android.material.R.id.open_search_view_scrim).run {
                setBackgroundColor(Color.TRANSPARENT)
            }
    }
}