package dev.brahmkshatriya.echo.ui.shelf.adapter.other

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemTrackListHeaderBinding
import dev.brahmkshatriya.echo.extensions.builtin.offline.MediaStoreUtils.searchBy
import dev.brahmkshatriya.echo.ui.common.PagingUtils
import dev.brahmkshatriya.echo.ui.common.PagingUtils.load
import dev.brahmkshatriya.echo.utils.CacheUtils.getFromCache
import dev.brahmkshatriya.echo.utils.CacheUtils.saveToCache
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.Sticky.Companion.sticky
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@SuppressLint("NotifyDataSetChanged")
class ShelfSearchHeaderAdapter(
    fragment: Fragment,
    private val viewModel: ViewModel,
    private val stateFlow: MutableStateFlow<PagingUtils.Data<Shelf>>,
    jobFlow: MutableStateFlow<Job?>
) : RecyclerView.Adapter<ShelfSearchHeaderAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTrackListHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        val ViewModel.shelfSortState by sticky { MutableStateFlow<ShelfSort.State?>(null) }
        val ViewModel.loadedShelves by sticky { MutableStateFlow<List<Shelf>?>(null) }
        private val ViewModel.loading by sticky { MutableStateFlow(false) }
        private val ViewModel.searchOpened by sticky { MutableStateFlow(false) }
        private val ViewModel.searchQuery by sticky { MutableStateFlow("") }
        private suspend fun loadTracks(
            current: MutableStateFlow<List<Shelf>?>,
            data: PagingUtils.Data<Shelf>,
        ): Result<List<Shelf>> = runCatching {
            val extension = data.extension ?: throw IllegalStateException("Extension is null")
            val paged = data.pagedData ?: throw IllegalStateException("PagedData is null")
            val list = mutableListOf<Shelf>()
            val maxCount = 1000
            var page = paged.load(extension, null).getOrThrow()
            list.addAll(page.data)
            current.value = list.toList()
            while (page.continuation != null && list.size < maxCount) {
                page = paged.load(extension, page.continuation).getOrThrow()
                list.addAll(page.data)
                current.value = list.toList()
            }
            list
        }

        private fun List<Shelf>.flatten() = flatMap { shelf ->
            when (shelf) {
                is Shelf.Category -> listOf(shelf)
                is Shelf.Item -> listOf(shelf)
                is Shelf.Lists.Categories -> shelf.list
                is Shelf.Lists.Items -> shelf.list.map { it.toShelf() }
                is Shelf.Lists.Tracks -> shelf.list.map { it.toMediaItem().toShelf() }
            }
        }

        private fun ViewModel.applyFilterAndSort(
            context: Context,
            stateFlow: MutableStateFlow<PagingUtils.Data<Shelf>>
        ) = viewModelScope.launch(Dispatchers.IO) {
            if (loading.value) return@launch
            loading.value = true
            stateFlow.value = stateFlow.value.copy(pagingData = PagingUtils.loadingPagingData())
            var shelves = loadedShelves.value
            if (shelves == null) {
                val data = stateFlow.value
                shelves = loadTracks(loadedShelves, data).getOrElse {
                    stateFlow.value =
                        stateFlow.value.copy(pagingData = PagingUtils.errorPagingData(it))
                    return@launch
                }
            }
            loading.value = false
            val id = stateFlow.value.id
            if (shelfSortState.value == null) {
                shelfSortState.value = getSortState(context, id) ?: ShelfSort.State()
            }
            val state = shelfSortState.value!!
            shelves = state.shelfSort?.sorter?.invoke(shelves.flatten()) ?: shelves
            if (state.reversed) shelves = shelves.reversed()

            if (searchQuery.value.isNotBlank()) {
                shelves = shelves.flatten().searchBy(searchQuery.value) { track ->
                    when (track) {
                        is Shelf.Category -> listOfNotNull(track.title, track.subtitle)
                        is Shelf.Item -> track.media.let { listOfNotNull(it.title, it.subtitle) }
                        is Shelf.Lists<*> -> throw IllegalStateException()
                    }
                }.map { it.second }
            }

            if (state.save) {
                context.saveToCache(id!!, state, "shelf_sort")
            }
            stateFlow.value =
                stateFlow.value.copy(pagingData = PagingUtils.from(shelves))
        }

        private fun getSortState(context: Context, id: String?) =
            id?.let { context.getFromCache<ShelfSort.State>(it, "shelf_sort") }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        binding.root.run {
            val text = if (viewModel.loadedShelves.value == null) null
            else {
                val count = viewModel.loadedShelves.value?.flatten()?.size ?: 0
                context.getString(R.string.x_items, count)
            }
            binding.title.text = text
            binding.searchLayout.hint = text
        }
        binding.loading.isVisible = viewModel.loading.value
        binding.titleContainer.isVisible = !viewModel.searchOpened.value
        binding.searchContainer.isVisible = viewModel.searchOpened.value
        binding.searchBar.setText(viewModel.searchQuery.value)
        binding.chipGroup.run {
            removeAllViews()
            val state = viewModel.shelfSortState.value
            if (state?.shelfSort != null) {
                val chip = Chip(context)
                chip.text = context.getString(state.shelfSort.title)
                chip.isCheckable = true
                chip.isChecked = true
                addView(chip)
                chip.setOnClickListener {
                    viewModel.shelfSortState.value = state.copy(shelfSort = null)
                    viewModel.applyFilterAndSort(context, stateFlow)
                }
            }
            if (state?.reversed == true) {
                val chip = Chip(context)
                chip.text = context.getString(R.string.reversed)
                chip.isCheckable = true
                chip.isChecked = true
                addView(chip)
                chip.setOnClickListener {
                    viewModel.shelfSortState.value = state.copy(reversed = false)
                    viewModel.applyFilterAndSort(context, stateFlow)
                }
            }
        }
    }

    private val fragmentManager = fragment.childFragmentManager

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTrackListHeaderBinding.inflate(inflater, parent, false)
        binding.btnSort.setOnClickListener {
            viewModel.applyFilterAndSort(parent.context, stateFlow)
            ShelfSortBottomSheet.newInstance(viewModel::class).show(fragmentManager, null)
        }
        binding.btnSearch.setOnClickListener {
            viewModel.searchOpened.value = true
            viewModel.applyFilterAndSort(parent.context, stateFlow)
        }
        binding.searchLayout.setEndIconOnClickListener {
            viewModel.searchOpened.value = false
            viewModel.searchQuery.value = ""
            viewModel.applyFilterAndSort(parent.context, stateFlow)
        }
        binding.searchBar.setOnEditorActionListener { _, _, _ ->
            viewModel.searchQuery.value = binding.searchBar.text.toString()
            viewModel.applyFilterAndSort(parent.context, stateFlow)
            true
        }
        return ViewHolder(binding)
    }

    private var visible: Boolean = false

    fun submit(visible: Boolean) {
        this.visible = visible
        notifyDataSetChanged()
    }

    override fun getItemCount() = if (visible) 1 else 0

    init {
        fragment.observe(stateFlow.map { it.pagedData }.distinctUntilChanged()) {
            if (it == null) return@observe
            val last = getSortState(fragment.requireContext(), stateFlow.value.id) ?: return@observe
            if (last.copy(save = false) == ShelfSort.State()) return@observe
            jobFlow.value?.cancel()
            viewModel.loadedShelves.value = null
            viewModel.applyFilterAndSort(fragment.requireContext(), stateFlow)
        }
        fragment.observe(viewModel.shelfSortState) {
            notifyDataSetChanged()
        }
        fragment.observe(viewModel.loadedShelves) {
            notifyDataSetChanged()
        }
        fragment.observe(viewModel.loading) {
            notifyDataSetChanged()
        }
        fragment.observe(viewModel.searchOpened) {
            notifyDataSetChanged()
        }
        fragment.observe(viewModel.searchQuery) {
            notifyDataSetChanged()
        }
        fragment.childFragmentManager.setFragmentResultListener("shelfSort", fragment) { _, _ ->
            viewModel.applyFilterAndSort(fragment.requireContext(), stateFlow)
        }
    }
}