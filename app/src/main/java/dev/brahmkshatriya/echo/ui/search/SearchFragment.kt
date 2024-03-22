package dev.brahmkshatriya.echo.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
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
    private val header = SearchHeaderAdapter {
        binding.catSearchView.setupWithSearchBar(it)
        binding.catSearchView.isVisible = true
    }
    private val concatAdapter = ConcatAdapter(header, adapter.withLoadingFooter())

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = FragmentSearchBinding.inflate(inflater, parent, false)
//        enterTransition = MaterialFade()
//        exitTransition = MaterialFade()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        PlayerBackButtonHelper.addCallback(this) {
            if (it == STATE_EXPANDED) binding.catSearchView.hide()
            binding.catRecyclerView.updatePaddingWithPlayerAndSystemInsets(it)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.statusBarScrim) { _, insets ->
            val i = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.statusBarScrim.updateLayoutParams { height = i.top }
            insets
        }
        postponeEnterTransition()
        binding.catRecyclerView.doOnPreDraw {
            startPostponedEnterTransition()
        }

        searchViewModel.query?.let { header.setText(it) }
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