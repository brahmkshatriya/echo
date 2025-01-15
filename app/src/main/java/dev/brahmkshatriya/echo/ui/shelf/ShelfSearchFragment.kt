package dev.brahmkshatriya.echo.ui.shelf

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.paging.PagingData
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.databinding.FragmentShelfSearchBinding
import dev.brahmkshatriya.echo.ui.adapter.ShelfAdapter
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.ui.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.ui.setupTransition
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets
import kotlinx.coroutines.Job

@AndroidEntryPoint
class ShelfSearchFragment : Fragment() {

    private var binding by autoCleared<FragmentShelfSearchBinding>()
    private val args by lazy { requireArguments() }
    private val clientId by lazy { args.getString("clientId")!! }
    private val title by lazy { args.getString("title")!! }
    private val wasSearch by lazy { args.getBoolean("wasSearch") }

    private val activityViewModel by activityViewModels<ShelfSearchViewModel>()
    private val viewModel by viewModels<ShelfSearchViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentShelfSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsets {
            binding.recyclerView.applyContentInsets(it)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
            binding.topBar.alpha = 1 - offset
        }
        binding.title.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        FastScrollerHelper.applyTo(binding.recyclerView)

        if (viewModel.shelves == null) {
            val shelves = activityViewModel.shelves ?: return
            activityViewModel.shelves = null
            viewModel.shelves = shelves
            viewModel.initialize()
        }

        binding.title.title = title

        binding.btnSort.setOnClickListener {
            createSnack(SnackBar.Message("Sort not implemented"))
        }

        binding.searchBar.setOnEditorActionListener { v, _, _ ->
            createSnack(SnackBar.Message(v.text.toString()))
            true
        }

        var shelfAdapter: ShelfAdapter? = null
        var job: Job? = null
        observe(activityViewModel.extensionListFlow) { list ->
            val extension = list?.find { it.metadata.id == clientId }
            extension ?: return@observe
            val adapter = ShelfAdapter(this, title, view.transitionName, extension)
            job?.cancel()
            job = adapter.applyCurrent(this, binding.recyclerView)
            shelfAdapter = adapter
            binding.recyclerView.adapter = adapter
        }

        observe(viewModel.flow) { data ->
            binding.loading.root.isVisible = data == null
            data ?: return@observe
            if (wasSearch) viewModel.run {
                if (searchBarClicked) return@run
                searchBarClicked = true
                binding.searchBar.performClick()
            }
            shelfAdapter?.submit(PagingData.from(data))
        }
    }

    companion object {
        fun newInstance(clientId: String, title: String, wasSearch: Boolean): Fragment {
            return ShelfSearchFragment().apply {
                arguments = Bundle().apply {
                    putString("clientId", clientId)
                    putString("title", title)
                    putBoolean("wasSearch", wasSearch)
                }
            }
        }
    }
}