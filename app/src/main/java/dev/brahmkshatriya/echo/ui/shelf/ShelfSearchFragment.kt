package dev.brahmkshatriya.echo.ui.shelf

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.paging.PagingData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentShelfSearchBinding
import dev.brahmkshatriya.echo.ui.adapter.ShelfAdapter
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.ui.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.ui.setupTransition
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
            val sorts = viewModel.sorts.flatMap {
                val sort = getString(it.title)
                listOf(sort, getString(R.string.sort_descending, sort))
            }
            val entries = (listOf(getString(R.string.video_quality_none)) + sorts).toTypedArray()
            val entryValues = (listOf(null to false) + viewModel.sorts.flatMap {
                listOf(it to false, it to true)
            }).toTypedArray()
            val value =
                viewModel.sort to if (viewModel.sort != null) viewModel.descending else false
            MaterialAlertDialogBuilder(requireContext())
                .setSingleChoiceItems(entries, entryValues.indexOf(value)) { dialog, index ->
                    val sort = entryValues[index]
                    viewModel.sort(sort.first, sort.second)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .setTitle(R.string.sort)
                .create()
                .show()
        }

        binding.searchBar.setText(viewModel.query)
        binding.searchBar.doOnTextChanged { text, _, _, _ ->
            viewModel.search(text.toString())
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
            viewModel.run {
                if (actionPerformed) return@run
                actionPerformed = true
                if (wasSearch) binding.searchBar.requestFocus()
                else binding.btnSort.performClick()
            }
            shelfAdapter?.submit(PagingData.from(data))
            binding.recyclerView.post { binding.recyclerView.scrollToPosition(0) }
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