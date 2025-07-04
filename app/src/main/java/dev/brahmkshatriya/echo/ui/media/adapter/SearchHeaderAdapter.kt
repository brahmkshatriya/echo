package dev.brahmkshatriya.echo.ui.media.adapter

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
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemTrackListHeaderBinding
import dev.brahmkshatriya.echo.extensions.builtin.offline.MediaStoreUtils.searchBy
import dev.brahmkshatriya.echo.ui.common.PagingUtils
import dev.brahmkshatriya.echo.ui.common.PagingUtils.load
import dev.brahmkshatriya.echo.ui.media.SortBottomSheet
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
class SearchHeaderAdapter(
    fragment: Fragment,
    private val viewModel: ViewModel,
    private val stateFlow: MutableStateFlow<PagingUtils.Data<Track>>,
    jobFlow: MutableStateFlow<Job?>
) : RecyclerView.Adapter<SearchHeaderAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTrackListHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        val ViewModel.trackSortState by sticky { MutableStateFlow<TrackSort.State?>(null) }
        val ViewModel.loadedTracks by sticky { MutableStateFlow<List<Track>?>(null) }
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
            context: Context,
            stateFlow: MutableStateFlow<PagingUtils.Data<Track>>
        ) = viewModelScope.launch(Dispatchers.IO) {
            if (loading.value) return@launch
            loading.value = true
            stateFlow.value = stateFlow.value.copy(pagingData = PagingUtils.loadingPagingData())
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
            val id = stateFlow.value.id
            if (trackSortState.value == null) {
                trackSortState.value = getSortState(context, id) ?: TrackSort.State()
            }
            val state = trackSortState.value!!
            tracks = state.trackSort?.sorter?.invoke(tracks) ?: tracks
            if (state.reversed) tracks = tracks.reversed()

            if (searchQuery.value.isNotBlank()) {
                tracks = tracks.searchBy(searchQuery.value) { track ->
                    listOf(track.title, *track.artists.map { it.name }.toTypedArray())
                }.map { it.second }
            }

            if (state.save) {
                context.saveToCache(id!!, state, "sort")
            }

            stateFlow.value =
                stateFlow.value.copy(pagingData = PagingUtils.from(tracks))
        }

        private fun getSortState(context: Context, id: String?) =
            id?.let { context.getFromCache<TrackSort.State>(it, "sort") }
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        binding.root.run {
            val text = if (viewModel.loadedTracks.value == null) context.getString(R.string.songs)
            else {
                val tracks = viewModel.loadedTracks.value?.size ?: 0
                runCatching {
                    resources.getQuantityString(R.plurals.n_songs, tracks, tracks)
                }.getOrNull() ?: resources.getString(R.string.x_songs, tracks)
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
            val state = viewModel.trackSortState.value
            if (state?.trackSort != null) {
                val chip = Chip(context)
                chip.text = context.getString(state.trackSort.title)
                chip.isCheckable = true
                chip.isChecked = true
                addView(chip)
                chip.setOnClickListener {
                    viewModel.trackSortState.value = state.copy(trackSort = null)
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
                    viewModel.trackSortState.value = state.copy(reversed = false)
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
            SortBottomSheet.newInstance(viewModel::class).show(fragmentManager, null)
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
            if (last.copy(save = false) == TrackSort.State()) return@observe
            jobFlow.value?.cancel()
            viewModel.loadedTracks.value = null
            viewModel.applyFilterAndSort(fragment.requireContext(), stateFlow)
        }
        fragment.observe(viewModel.trackSortState) {
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
        fragment.childFragmentManager.setFragmentResultListener("sort", fragment) { _, _ ->
            viewModel.applyFilterAndSort(fragment.requireContext(), stateFlow)
        }
    }
}