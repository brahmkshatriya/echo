package dev.brahmkshatriya.echo.ui.playlist.save

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.databinding.DialogPlaylistSaveBinding
import dev.brahmkshatriya.echo.ui.media.adapter.GenericItemAdapter
import dev.brahmkshatriya.echo.ui.media.adapter.MediaItemSelectableAdapter
import dev.brahmkshatriya.echo.ui.media.adapter.MediaItemSelectableAdapter.Companion.mediaItemSpanCount
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.playlist.CreatePlaylistBottomSheet
import dev.brahmkshatriya.echo.ui.shelf.adapter.MediaItemViewHolder
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import kotlinx.coroutines.flow.combine
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SaveToPlaylistBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(extensionId: String, item: EchoMediaItem) =
            SaveToPlaylistBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("extensionId", extensionId)
                    putSerialized("item", item)
                }
            }
    }

    private val args by lazy { requireArguments() }
    private val extensionId by lazy { args.getString("extensionId")!! }
    private val item: EchoMediaItem by lazy { args.getSerialized("item")!! }

    private val itemAdapter by lazy {
        GenericItemAdapter(
            playerViewModel.playerState.current,
            object : MediaItemViewHolder.Listener {
                override fun onMediaItemClicked(
                    extensionId: String?, item: EchoMediaItem?, it: View?
                ) {
                }
            }
        )
    }

    private val adapter by lazy {
        MediaItemSelectableAdapter { _, item ->
            item as EchoMediaItem.Lists.PlaylistItem
            viewModel.togglePlaylist(item.playlist)
        }
    }

    private val topBarAdapter by lazy {
        TopAppBarAdapter(
            { dismiss() },
            { CreatePlaylistBottomSheet().show(parentFragmentManager, null) }
        )
    }

    private val bottomSaveAdapter by lazy {
        SaveButtonAdapter {
            viewModel.saveTracks()
        }
    }

    private var binding by autoCleared<DialogPlaylistSaveBinding>()
    private val viewModel by viewModel<SaveToPlaylistViewModel> { parametersOf(extensionId, item) }
    private val playerViewModel by activityViewModel<PlayerViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogPlaylistSaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var count = 1
        val gridLayoutManager = GridLayoutManager(requireContext(), count)
        binding.recyclerView.mediaItemSpanCount {
            count = it
            gridLayoutManager.spanCount = it
            adapter.submitList(adapter.currentList)
        }
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val last = adapter.itemCount + 3
                return when (position) {
                    0, 1, 2, last -> count
                    else -> 1
                }
            }
        }
        itemAdapter.submitList(extensionId, listOf(item))
        binding.recyclerView.layoutManager = gridLayoutManager
        binding.recyclerView.adapter = ConcatAdapter(
            topBarAdapter,
            itemAdapter,
            adapter.withHeader { viewModel.toggleAll(it) },
            bottomSaveAdapter
        )

        val combined = viewModel.playlistsFlow.combine(viewModel.saveFlow) { playlists, save ->
            playlists to save
        }
        observe(combined) { (state, save) ->
            val playlistLoading = when (state) {
                is SaveToPlaylistViewModel.PlaylistState.Loaded -> {
                    if (state.list == null) {
                        dismiss()
                        return@observe
                    }
                    adapter.submitList(state.list.map { it.first.toMediaItem() to it.second })
                    bottomSaveAdapter.setEnabled(state.list.any { it.second })
                    false
                }

                else -> true
            }
            val saving = save != SaveToPlaylistViewModel.SaveState.Initial
            val loading = playlistLoading || saving
            binding.recyclerView.isVisible = !loading
            binding.loading.root.isVisible = loading
            binding.loading.textView.text = when (save) {
                SaveToPlaylistViewModel.SaveState.Initial -> getString(R.string.loading)
                is SaveToPlaylistViewModel.SaveState.LoadingPlaylist ->
                    getString(R.string.loading_x, save.playlist.title)

                SaveToPlaylistViewModel.SaveState.LoadingTracks -> getString(
                    R.string.loading_x,
                    item.title
                )

                is SaveToPlaylistViewModel.SaveState.Saved -> {
                    dismiss()
                    getString(R.string.not_loading)
                }

                is SaveToPlaylistViewModel.SaveState.Saving ->
                    getString(R.string.saving_x, save.playlist.title)
            }
        }

        parentFragmentManager.setFragmentResultListener(
            "createPlaylist", this
        ) { _, _ -> viewModel.refresh() }
    }
}