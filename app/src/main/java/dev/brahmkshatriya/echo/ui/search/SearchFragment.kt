package dev.brahmkshatriya.echo.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.databinding.FragmentSearchBinding
import dev.brahmkshatriya.echo.player.ui.PlayerBackButtonHelper
import dev.brahmkshatriya.echo.ui.adapters.MediaItemsContainerAdapter
import dev.brahmkshatriya.echo.ui.adapters.SearchHeaderAdapter
import dev.brahmkshatriya.echo.ui.extension.getAdapterForExtension
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.updatePaddingWithPlayerAndSystemInsets

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var binding: FragmentSearchBinding by autoCleared()
    private val searchViewModel: SearchViewModel by activityViewModels()

    private val adapter = MediaItemsContainerAdapter(this)
    private val header = SearchHeaderAdapter(searchViewModel.query) {
        binding.catSearchView.setupWithSearchBar(it)
    }
    private val concatAdapter = ConcatAdapter(header, adapter)

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = FragmentSearchBinding.inflate(inflater, parent, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        PlayerBackButtonHelper.addCallback(this) {
            if (it == STATE_EXPANDED) binding.catSearchView.hide()
            binding.catRecyclerView.updatePaddingWithPlayerAndSystemInsets(it)
        }

        binding.catSearchView.editText.setOnEditorActionListener { textView, _, _ ->
            textView.text.toString().ifBlank { null }?.let {
                searchViewModel.search(it)
                header.setText(it)
                binding.catSearchView.hide()
            }
            false
        }
        binding.catRecyclerView.layoutManager = LinearLayoutManager(context)
        observe(searchViewModel.searchFlow.flow) {
            binding.catRecyclerView.adapter = getAdapterForExtension<SearchClient>(
                it, R.string.search, concatAdapter
            ) {}
        }
        observe(searchViewModel.result) {
            if (it != null) adapter.submit(it)
        }
    }
}