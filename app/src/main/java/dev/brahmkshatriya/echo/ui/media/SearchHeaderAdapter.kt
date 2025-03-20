package dev.brahmkshatriya.echo.ui.media

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemTrackListHeaderBinding
import dev.brahmkshatriya.echo.extensions.builtin.offline.MediaStoreUtils.searchBy
import dev.brahmkshatriya.echo.ui.common.PagingUtils
import dev.brahmkshatriya.echo.ui.common.PagingUtils.load
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.Sticky.Companion.sticky
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@SuppressLint("NotifyDataSetChanged")
class SearchHeaderAdapter(
    fragment: Fragment,
    private val viewModel: ViewModel,
    private val stateFlow: MutableStateFlow<PagingUtils.Data<Track>>
) : RecyclerView.Adapter<SearchHeaderAdapter.ViewHolder>() {

    val fragmentManager = fragment.childFragmentManager

    class ViewHolder(val binding: ItemTrackListHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        private val ViewModel.sortState by sticky { MutableStateFlow(Sort.State()) }
        private val ViewModel.loadedTracks by sticky { MutableStateFlow<List<Track>?>(null) }
        private val ViewModel.loading by sticky { MutableStateFlow(false) }
        private val ViewModel.searchOpened by sticky { MutableStateFlow(false) }
        private val ViewModel.searchQuery by sticky { MutableStateFlow("") }
        private suspend fun loadTracks(
            current: MutableStateFlow<List<Track>?>,
            data: PagingUtils.Data<Track>,
        ): Result<List<Track>> = runCatching {
            val extension = data.extension ?: throw IllegalStateException("Extension is null")
            val paged = data.pagedData ?: throw IllegalStateException("PagedData is null")
            val list = mutableListOf<Track>()
            val maxCount = 5000
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

        private fun ViewModel.applyFilterAndSort(
            stateFlow: MutableStateFlow<PagingUtils.Data<Track>>
        ) = viewModelScope.launch(Dispatchers.IO) {
            if (loading.value) return@launch
            loading.value = true
            stateFlow.value = stateFlow.value.copy(pagingData = PagingData.empty())
            var tracks = loadedTracks.value
            if (tracks == null) {
                val data = stateFlow.value
                tracks = loadTracks(loadedTracks, data).getOrElse {
                    stateFlow.value =
                        stateFlow.value.copy(pagingData = PagingUtils.errorPagingData(it))
                    return@launch
                }
            }
            loading.value = false
            val state = sortState.value
            tracks = state.sort?.sorter?.invoke(tracks) ?: tracks
            if (state.reversed) tracks = tracks.reversed()

            if (searchQuery.value.isNotBlank()) {
                tracks = tracks.searchBy(searchQuery.value) { track ->
                    listOf(track.title, *track.artists.map { it.name }.toTypedArray())
                }.map { it.second }
            }

            stateFlow.value =
                stateFlow.value.copy(pagingData = PagingData.from(tracks))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        binding.root.run {
            val text = if (viewModel.loadedTracks.value == null) context.getString(R.string.songs)
            else {
                val count = viewModel.loadedTracks.value?.size ?: 0
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
            val state = viewModel.sortState.value
            if (state.sort != null) {
                val chip = Chip(context)
                chip.text = context.getString(state.sort.title)
                chip.isCheckable = true
                chip.isChecked = true
                addView(chip)
                chip.setOnClickListener {
                    viewModel.sortState.value = state.copy(sort = null)
                    viewModel.applyFilterAndSort(stateFlow)
                }
            }
            if (state.reversed) {
                val chip = Chip(context)
                chip.text = context.getString(R.string.reversed)
                chip.isCheckable = true
                chip.isChecked = true
                addView(chip)
                chip.setOnClickListener {
                    viewModel.sortState.value = state.copy(reversed = false)
                    viewModel.applyFilterAndSort(stateFlow)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTrackListHeaderBinding.inflate(inflater, parent, false)
        binding.btnSort.setOnClickListener {
            viewModel.applyFilterAndSort(stateFlow)
            SortBottomSheet(
                sortState = viewModel.sortState,
                loadedTracks = viewModel.loadedTracks,
                apply = { viewModel.applyFilterAndSort(stateFlow) }
            ).showNow(fragmentManager, null)
        }
        binding.btnSearch.setOnClickListener {
            viewModel.searchOpened.value = true
            viewModel.applyFilterAndSort(stateFlow)
        }
        binding.searchLayout.setEndIconOnClickListener {
            viewModel.searchOpened.value = false
            viewModel.searchQuery.value = ""
            viewModel.applyFilterAndSort(stateFlow)
        }
        binding.searchBar.setOnEditorActionListener { _, _, _ ->
            viewModel.searchQuery.value = binding.searchBar.text.toString()
            viewModel.applyFilterAndSort(stateFlow)
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
        fragment.observe(viewModel.sortState) {
            println("sort state changed")
            notifyDataSetChanged()
        }
        fragment.observe(viewModel.loadedTracks) {
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
    }
}