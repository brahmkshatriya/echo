package dev.brahmkshatriya.echo.ui.editplaylist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.PlaylistEditCoverClient
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.FragmentEditPlaylistBinding
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.extensions.isClient
import dev.brahmkshatriya.echo.ui.player.PlaylistAdapter
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.editplaylist.EditPlaylistViewModel.ListAction.Add
import dev.brahmkshatriya.echo.ui.editplaylist.EditPlaylistViewModel.ListAction.Move
import dev.brahmkshatriya.echo.ui.editplaylist.EditPlaylistViewModel.ListAction.Remove
import dev.brahmkshatriya.echo.utils.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.getSerialized
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.putSerialized
import dev.brahmkshatriya.echo.utils.setupTransition
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyFabInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets

class EditPlaylistFragment : Fragment() {

    companion object {
        fun newInstance(
            client: String,
            playlist: Playlist
        ): EditPlaylistFragment {
            check(playlist.isEditable)
            return EditPlaylistFragment().apply {
                arguments = Bundle().apply {
                    putString("clientId", client)
                    putSerialized("playlist", playlist)
                }
            }
        }
    }

    private val args by lazy { requireArguments() }
    private val clientId by lazy { args.getString("clientId")!! }
    private val playlist by lazy { args.getSerialized<Playlist>("playlist")!! }
    var binding by autoCleared<FragmentEditPlaylistBinding>()
    val viewModel by activityViewModels<EditPlaylistViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsets {
            binding.toolbarIconContainer.updatePadding(top = it.top)
            binding.recyclerView.updatePaddingRelative(
                top = 16.dpToPx(requireContext()),
                bottom = it.bottom + 96.dpToPx(requireContext()),
                start = it.start,
                end = it.end
            )
            binding.fabContainer.applyFabInsets(it, systemInsets.value)
        }

        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.onEditorExit(clientId, playlist)
            }
        }

        fun onLoadChange(it: Boolean?) = when (it) {
            true -> {
                binding.loading.root.isVisible = true
                binding.recyclerView.isVisible = false
                binding.fabContainer.isVisible = false
            }

            false -> parentFragmentManager.popBackStack()
            else -> {
                binding.loading.root.isVisible = false
                binding.recyclerView.isVisible = true
                binding.fabContainer.isVisible = true
            }
        }
        observe(viewModel.loadingFlow) { onLoadChange(it) }
        onLoadChange(viewModel.loading)

        observe(viewModel.performedActions) { (tracks, action) ->
            println("action : $action")
            binding.loading.textView.text = when (action) {
                is Add -> getString(
                    R.string.adding_tracks,
                    action.items.joinToString(", ") { it.title }
                )

                is Move -> getString(
                    R.string.moving_track, tracks[action.to].title
                )

                is Remove -> getString(
                    R.string.removing_track,
                    action.indexes.joinToString(", ") { tracks[it].title }
                )

                else -> getString(R.string.saving)
            }

        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
        applyBackPressCallback()

        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
        }

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.fabAddTracks.setOnClickListener {
            backCallback.isEnabled = false
            openFragment(SearchForPlaylistFragment.newInstance(clientId, playlist), it)
        }
        parentFragmentManager.setFragmentResultListener(
            "searchedTracks",
            viewLifecycleOwner
        ) { _, bundle ->
            val tracks = bundle.getSerialized<List<Track>>("tracks")!!.toMutableList()
            viewModel.edit(Add(viewModel.currentTracks.value?.size ?: 0, tracks))
            backCallback.isEnabled = true
        }

        binding.loading.root.isVisible = true
        binding.recyclerView.isVisible = false
        val header = EditPlaylistHeader(playlist, object : EditPlaylistHeader.Listener {
            override fun onCoverClicked() {
                createSnack("Cover Change Not Implemented")
            }

            override fun onUpdate(title: String, description: String?) {
                viewModel.changeMetadata(clientId, playlist, title, description)
            }
        })
        observe(viewModel.extensionListFlow) {
            viewModel.load(clientId, playlist)
            val extension = viewModel.extensionListFlow.getExtension(clientId) ?: return@observe
            header.showCover(extension.isClient<PlaylistEditCoverClient>())
        }

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.deletePlaylist -> {
                    viewModel.deletePlaylist(clientId, playlist)
                    parentFragmentManager.popBackStack()
                    parentFragmentManager.setFragmentResult("deleted", Bundle().apply {
                        putString("id", playlist.id)
                    })
                    true
                }

                else -> false
            }
        }


        // Playlist Songs
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.START
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (viewHolder is EditPlaylistHeader.ViewHolder) return false
                if (target is EditPlaylistHeader.ViewHolder) return false

                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                viewModel.edit(Move(fromPos, toPos))
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                viewModel.edit(Remove(pos))
            }
        }
        val touchHelper = ItemTouchHelper(callback)
        val adapter = PlaylistAdapter(object : PlaylistAdapter.Callback() {
            override fun onDragHandleTouched(viewHolder: RecyclerView.ViewHolder) {
                touchHelper.startDrag(viewHolder)
            }

            override fun onItemClicked(position: Int) {}

            override fun onItemClosedClicked(position: Int) {
                viewModel.edit(Remove(position))
            }
        })


        binding.recyclerView.adapter = ConcatAdapter(header, adapter)
        touchHelper.attachToRecyclerView(binding.recyclerView)
        FastScrollerHelper.applyTo(binding.recyclerView)

        observe(viewModel.currentTracks) { tracks ->
            tracks?.let {
                adapter.submitList(it.map { track ->
                    false to MediaItemUtils.build(null, track, clientId, null)
                })
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        parentFragmentManager.setFragmentResult("reload", bundleOf("id" to playlist.id))
    }
}
