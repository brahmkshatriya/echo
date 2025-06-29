package dev.brahmkshatriya.echo.ui.playlist.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.FragmentPlaylistEditBinding
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsetsMain
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.playlist.edit.search.EditPlaylistSearchFragment
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.ui.UiUtils.configureAppBar
import kotlinx.coroutines.flow.combine
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class EditPlaylistFragment : Fragment() {

    companion object {
        fun getBundle(extension: String, playlist: Playlist, loaded: Boolean) = Bundle().apply {
            putString("extensionId", extension)
            putSerialized("playlist", playlist)
            putBoolean("loaded", loaded)
        }
    }

    private val args by lazy { requireArguments() }
    private val extensionId by lazy { args.getString("extensionId")!! }
    private val playlist by lazy { args.getSerialized<Playlist>("playlist")!! }
    private val loaded by lazy { args.getBoolean("loaded", false) }

    private var binding: FragmentPlaylistEditBinding by autoCleared()
    private val playerViewModel by activityViewModel<PlayerViewModel>()
    private val vm by viewModel<EditPlaylistViewModel> {
        parametersOf(extensionId, playlist, loaded)
    }

    private val adapter by lazy {
        val itemCallback = PlaylistAdapter.getTouchHelper(vm)
        itemCallback.attachToRecyclerView(binding.recyclerView)
        PlaylistAdapter(playerViewModel.playerState.current, object : PlaylistAdapter.Listener {
            override fun onItemClosedClicked(viewHolder: PlaylistAdapter.ViewHolder) {
                vm.edit(
                    EditPlaylistViewModel.Action.Remove(listOf(viewHolder.bindingAdapterPosition))
                )
            }

            override fun onDragHandleTouched(viewHolder: PlaylistAdapter.ViewHolder) {
                itemCallback.startDrag(viewHolder)
            }
        })
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaylistEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsetsMain(binding.appBarLayout, binding.recyclerView, 96) {
            binding.fabContainer.applyInsets(it)
        }

        applyBackPressCallback()
        binding.appBarLayout.configureAppBar { offset ->
            binding.toolbarOutline.alpha = offset
            binding.toolbarIconContainer.alpha = 1 - offset
        }

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.toolbar.setOnMenuItemClickListener {
            parentFragmentManager.setFragmentResult("delete", Bundle().apply {
                putSerialized("playlist", playlist)
            })
            parentFragmentManager.popBackStack()
            true
        }

        binding.save.setOnClickListener {
            vm.save()
        }

        binding.add.setOnClickListener {
            openFragment<EditPlaylistSearchFragment>(
                it, EditPlaylistSearchFragment.getBundle(extensionId)
            )
        }
        parentFragmentManager.setFragmentResultListener("searchedTracks", this) { _, bundle ->
            val tracks = bundle.getSerialized<List<Track>>("tracks")!!.toMutableList()
            vm.edit(
                EditPlaylistViewModel.Action.Add(
                    vm.currentTracks.value?.size ?: 0, tracks
                )
            )
        }

        FastScrollerHelper.applyTo(binding.recyclerView)
        binding.recyclerView.adapter = ConcatAdapter(
            EditPlaylistHeaderAdapter(vm), adapter
        )

        observe(vm.currentTracks) {
            adapter.submitList(it)
        }
        val combined = vm.originalList.combine(vm.saveState) { a, b -> a to b }
        observe(combined) { (tracks, save) ->
            val trackLoading = tracks == EditPlaylistViewModel.LoadingState.Loading
            val saving = save != EditPlaylistViewModel.SaveState.Initial
            val loading = trackLoading || saving
            binding.recyclerView.isVisible = !loading
            binding.fabContainer.isVisible = !loading
            binding.loading.root.isVisible = loading
            binding.loading.textView.text = when (save) {
                is EditPlaylistViewModel.SaveState.Performing -> when (save.action) {
                    is EditPlaylistViewModel.Action.Add ->
                        getString(R.string.adding_x, save.tracks.joinToString(", ") { it.title })

                    is EditPlaylistViewModel.Action.Move ->
                        getString(R.string.moving_x, save.tracks.first().title)

                    is EditPlaylistViewModel.Action.Remove ->
                        getString(R.string.removing_x, save.tracks.joinToString(", ") { it.title })
                }

                EditPlaylistViewModel.SaveState.Saving ->
                    getString(R.string.saving_x, playlist.title)

                EditPlaylistViewModel.SaveState.Initial -> getString(R.string.loading)
                is EditPlaylistViewModel.SaveState.Saved -> {
                    if (save.success) parentFragmentManager.setFragmentResult(
                        "reload", bundleOf("id" to playlist.id)
                    )
                    parentFragmentManager.popBackStack()
                    getString(R.string.loading)
                }
            }
        }
    }
}