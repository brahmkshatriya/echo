package dev.brahmkshatriya.echo.ui.search

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.databinding.FragmentSearchBinding
import dev.brahmkshatriya.echo.ui.adapters.MediaItemAdapter
import dev.brahmkshatriya.echo.ui.adapters.MediaItemComparator
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.utils.updateBottomMarginWithSystemInsets
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private val searchViewModel: SearchViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = FragmentSearchBinding.inflate(inflater, parent, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PlayerViewModel.handleBackPress(this) {
            if (!it) binding.catSearchView.hide()
        }
        updateBottomMarginWithSystemInsets(binding.root)

        binding.catSearchView.setupWithSearchBar(binding.catSearchBar)
        binding.catSearchView.editText.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                textView.text.toString().ifBlank { null }?.let {
                    observeSearchFlow(it)
                    binding.catSearchView.hide()
                    return@setOnEditorActionListener true
                }
            }
            false
        }
        binding.catRecyclerView.layoutManager = GridLayoutManager(context, 3)
        binding.catRecyclerView.adapter = adapter
    }

    private val adapter = MediaItemAdapter(MediaItemComparator)
    private fun observeSearchFlow(query: String) {
        lifecycleScope.launch {
            searchViewModel.search(query).collectLatest { pagingData ->
                (binding.catRecyclerView.adapter as MediaItemAdapter).submitData(pagingData)
            }
        }
    }
}