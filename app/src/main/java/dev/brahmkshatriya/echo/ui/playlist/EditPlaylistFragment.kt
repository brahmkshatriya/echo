package dev.brahmkshatriya.echo.ui.playlist

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.EditPlaylistCoverClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.databinding.FragmentEditPlaylistBinding
import dev.brahmkshatriya.echo.playback.Queue
import dev.brahmkshatriya.echo.ui.media.MediaItemViewHolder.Companion.placeHolder
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.setupTransition
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets

class EditPlaylistFragment : Fragment() {

    companion object {
        fun newInstance(client: String, playlist: Playlist): EditPlaylistFragment {
            check(playlist.isEditable)
            return EditPlaylistFragment().apply {
                arguments = Bundle().apply {
                    putString("clientId", client)
                    putParcelable("playlist", playlist)
                }
            }
        }
    }

    private val args by lazy { requireArguments() }
    private val clientId by lazy { args.getString("clientId")!! }
    private val client by lazy { viewModel.extensionListFlow.getClient(clientId) }

    @Suppress("DEPRECATION")
    private val playlist by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) args.getParcelable(
            "playlist",
            Playlist::class.java
        )!!
        else args.getParcelable("playlist")!!
    }
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
            binding.nestedScrollView.applyInsets(it)
            binding.fabContainer.applyInsets(it)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
        }

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        viewModel.onEditorEnter(clientId, playlist)

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

        //TODO Cover Selection
        binding.coverContainer.isVisible = client is EditPlaylistCoverClient
        playlist.cover.loadInto(binding.cover, playlist.toMediaItem().placeHolder())
        binding.cover.setOnClickListener {
            createSnack("Cover Change Not Implemented")
        }

        fun updateMetadata() = viewModel.changeMetadata(
            clientId,
            playlist,
            binding.playlistName.text.toString(),
            binding.playlistDescription.text.toString().ifEmpty { null }
        )
        binding.playlistName.apply {
            setText(playlist.title)
            setOnEditorActionListener { _, _, _ -> updateMetadata();false }
        }
        binding.playlistDescription.apply {
            setText(playlist.description)
            setOnEditorActionListener { _, _, _ -> updateMetadata();false }
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
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                viewModel.moveTracks(clientId, playlist, fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                viewModel.removeTracks(clientId, playlist, listOf(pos))
            }
        }
        val touchHelper = ItemTouchHelper(callback)
        val adapter = PlaylistAdapter(null, object : PlaylistAdapter.Callback() {
            override fun onDragHandleTouched(viewHolder: RecyclerView.ViewHolder) {
                touchHelper.startDrag(viewHolder)
            }

            override fun onItemClicked(position: Int) {}

            override fun onItemClosedClicked(position: Int) {
                viewModel.removeTracks(clientId, playlist, listOf(position))
            }
        })

        binding.playlistSongs.adapter = adapter
        touchHelper.attachToRecyclerView(binding.playlistSongs)

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
        viewModel.onEditorExit(clientId, playlist)
        parentFragmentManager.setFragmentResult("reload", Bundle().apply {
            putString("id", playlist.id)
        })
    }
}
