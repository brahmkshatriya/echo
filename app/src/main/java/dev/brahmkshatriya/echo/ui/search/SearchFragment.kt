package dev.brahmkshatriya.echo.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.databinding.FragmentSearchBinding
import dev.brahmkshatriya.echo.player.PlayerViewModel
import dev.brahmkshatriya.echo.player.ui.PlayerBackButtonHelper
import dev.brahmkshatriya.echo.ui.adapters.MediaItemListener
import dev.brahmkshatriya.echo.ui.adapters.MediaItemsContainerAdapter
import dev.brahmkshatriya.echo.ui.adapters.SearchHeaderAdapter
import dev.brahmkshatriya.echo.ui.extension.getAdapterForExtension
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.updatePaddingWithSystemInsets

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var binding: FragmentSearchBinding by autoCleared()
    private val searchViewModel: SearchViewModel by activityViewModels()
    private val playerViewModel: PlayerViewModel by activityViewModels()


    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = FragmentSearchBinding.inflate(inflater, parent, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        PlayerBackButtonHelper.addCallback(this) {
            if (!it) binding.catSearchView.hide()
        }

        updatePaddingWithSystemInsets(binding.catRecyclerView)

        val header = SearchHeaderAdapter(searchViewModel.query) {
            binding.catSearchView.setupWithSearchBar(it)
        }

        binding.catSearchView.editText.setOnEditorActionListener { textView, _, _ ->
            textView.text.toString().ifBlank { null }?.let {
                searchViewModel.search(it)
                header.setText(it)
                binding.catSearchView.hide()
            }
            false
        }
        val adapter = MediaItemsContainerAdapter(
            lifecycle, MediaItemListener(findNavController(), playerViewModel)
        )

        val concatAdapter = ConcatAdapter(header, adapter)
        binding.catRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        observe(searchViewModel.searchFlow.flow) {
            binding.catRecyclerView.adapter = getAdapterForExtension<SearchClient>(
                it, R.string.search, concatAdapter
            ) {}
        }
        observe(searchViewModel.result) {
            if (it != null) adapter.submitData(it)
        }
    }
}