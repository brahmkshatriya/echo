package dev.brahmkshatriya.echo.ui.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.EditPlaylistCoverClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.FragmentEditPlaylistBinding
import dev.brahmkshatriya.echo.playback.Queue
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.media.MediaItemViewHolder.Companion.placeHolder
import dev.brahmkshatriya.echo.utils.ListAction
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.getParcel
import dev.brahmkshatriya.echo.utils.getParcelArray
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.setupTransition
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyFabInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets
import kotlinx.coroutines.launch

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
    private val playlist by lazy { args.getParcel<Playlist>("playlist")!! }

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
            binding.fabContainer.applyFabInsets(it, systemInsets.value)
        }

        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

                lifecycleScope.launch {
                    binding.nestedScrollView.isVisible = false
                    binding.loading.root.isVisible = true
                    viewModel.onEditorExit(clientId, playlist) { action ->
                        println("action : $action")
                        binding.loading.textView.text = when (action) {
                            is ListAction.Add -> getString(
                                R.string.adding_tracks,
                                action.items.joinToString(", ") { it.title }
                            )

                            is ListAction.Move -> getString(
                                R.string.moving_track, playlist.tracks[action.from].title
                            )

                            is ListAction.Remove -> getString(
                                R.string.removing_tracks,
                                action.items.joinToString(", ") { it.title }
                            )

                            else -> getString(R.string.saving)
                        }
                    }
                    parentFragmentManager.popBackStack()
                }
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
            val tracks = bundle.getParcelArray<Track>("tracks")!!
            viewModel.edit { addAll(tracks) }
            backCallback.isEnabled = true
        }

        binding.loading.root.isVisible = true
        binding.nestedScrollView.isVisible = false

        observe(viewModel.extensionListFlow.flow) {
            viewModel.currentTracks.apply {
                if (value == null) {
                    value = playlist.tracks
                    viewModel.onEditorEnter(clientId, playlist)
                }
            }

            val client = viewModel.extensionListFlow.getClient(clientId)
            binding.coverContainer.isVisible = client is EditPlaylistCoverClient

            binding.loading.root.isVisible = false
            binding.nestedScrollView.isVisible = true
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

        //TODO Cover Selection
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
                viewModel.edit { add(toPos, removeAt(fromPos)) }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                viewModel.edit { removeAt(pos) }
            }
        }
        val touchHelper = ItemTouchHelper(callback)
        val adapter = PlaylistAdapter(null, object : PlaylistAdapter.Callback() {
            override fun onDragHandleTouched(viewHolder: RecyclerView.ViewHolder) {
                touchHelper.startDrag(viewHolder)
            }

            override fun onItemClicked(position: Int) {}

            override fun onItemClosedClicked(position: Int) {
                viewModel.edit { removeAt(position) }
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
        parentFragmentManager.setFragmentResult("reload", Bundle().apply {
            putString("id", playlist.id)
        })
    }
}
