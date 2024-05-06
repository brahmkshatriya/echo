package dev.brahmkshatriya.echo.ui.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.EditPlaylistCoverClient
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.FragmentEditPlaylistBinding
import dev.brahmkshatriya.echo.playback.Queue
import dev.brahmkshatriya.echo.plugger.getClient
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.playlist.EditPlaylistViewModel.ListAction.Add
import dev.brahmkshatriya.echo.ui.playlist.EditPlaylistViewModel.ListAction.Move
import dev.brahmkshatriya.echo.ui.playlist.EditPlaylistViewModel.ListAction.Remove
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.getParcel
import dev.brahmkshatriya.echo.utils.getParcelArray
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.setupTransition
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyFabInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets

class EditPlaylistFragment : Fragment() {

    companion object {
        fun newInstance(
            client: String,
            playlist: Playlist,
            tracks: List<Track>
        ): EditPlaylistFragment {
            check(playlist.isEditable)
            return EditPlaylistFragment().apply {
                arguments = Bundle().apply {
                    putString("clientId", client)
                    putParcelable("playlist", playlist)
                    putParcelableArray("tracks", tracks.toTypedArray())
                }
            }
        }
    }

    private val args by lazy { requireArguments() }
    private val clientId by lazy { args.getString("clientId")!! }
    private val playlist by lazy { args.getParcel<Playlist>("playlist")!! }
    private val tracks by lazy { args.getParcelArray<Track>("tracks")!! }

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
                top = 0,
                bottom = it.bottom + 96.dpToPx(requireContext()),
                start = it.start,
                end = it.end
            )
            binding.fabContainer.applyFabInsets(it, systemInsets.value)
        }

        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.closing.value == true) return
                viewModel.closing.value = true
                viewModel.onEditorExit(clientId, playlist) { action ->
                    println("action : $action")
                    binding.loading.textView.text = when (action) {
                        is Add -> getString(
                            R.string.adding_tracks,
                            action.items.joinToString(", ") { it.title }
                        )

                        is Move -> getString(
                            R.string.moving_track, action.to.toString()
                        )

                        is Remove -> getString(
                            R.string.removing_track,
                            action.indexes.toString()
                        )

                        else -> getString(R.string.saving)
                    }
                }
                viewModel.closing.value = false
            }
        }
        observe(viewModel.closing) {
            when (it) {
                true -> {
                    binding.recyclerView.isVisible = false
                    binding.loading.root.isVisible = true
                }

                false -> parentFragmentManager.popBackStack()
                else -> {}
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
            val tracks = bundle.getParcelArray<Track>("tracks")!!.toMutableList()
            viewModel.edit(Add(playlist.tracks?.minus(1) ?: 0, tracks))
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
            viewModel.currentTracks.apply {
                if (value == null) {
                    value = tracks
                    viewModel.onEditorEnter(clientId, playlist)
                }
            }

            val client = viewModel.extensionListFlow.getClient(clientId)?.client
            header.showCover(client is EditPlaylistCoverClient)

            binding.loading.root.isVisible = false
            binding.recyclerView.isVisible = true
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
        val adapter = PlaylistAdapter(null, object : PlaylistAdapter.Callback() {
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

        observe(viewModel.currentTracks) { tracks ->
            tracks?.let {
                adapter.submitList(it.map { track ->
                    Queue.StreamableTrack(track, clientId)
                })
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.currentTracks.value = null
        parentFragmentManager.setFragmentResult("reload", Bundle().apply {
            putString("id", playlist.id)
        })
    }
}
