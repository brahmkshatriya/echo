package dev.brahmkshatriya.echo.ui.playlist

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.DialogAddToPlaylistBinding
import dev.brahmkshatriya.echo.ui.media.MediaItemSelectableAdapter
import dev.brahmkshatriya.echo.ui.media.MediaItemSelectableAdapter.Companion.mediaItemSpanCount
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe

@AndroidEntryPoint
class AddToPlaylistBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(clientId: String, album: Album) = AddToPlaylistBottomSheet().apply {
            arguments = Bundle().apply {
                putString("clientId", clientId)
                putParcelable("album", album)
            }
        }

        fun newInstance(clientId: String, track: Track) = AddToPlaylistBottomSheet().apply {
            arguments = Bundle().apply {
                putString("clientId", clientId)
                putParcelable("album", Album("", "", tracks = listOf(track)))
            }
        }
    }

    private val args by lazy { requireArguments() }
    private val clientId by lazy { args.getString("clientId")!! }

    @Suppress("DEPRECATION")
    private val album: Album by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            requireArguments().getParcelable("album", Album::class.java)!!
        else args.getParcelable("album")!!
    }

    var binding by autoCleared<DialogAddToPlaylistBinding>()
    val viewModel by viewModels<AddToPlaylistViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogAddToPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.save.setOnClickListener {
            viewModel.saving = true
            binding.loading.root.isVisible = true
            binding.loading.textView.text = getString(R.string.saving)
            binding.nestedScrollView.isVisible = false
            viewModel.addToPlaylists(album.tracks)
        }

        val adapter = MediaItemSelectableAdapter { _, item ->
            item as EchoMediaItem.Lists.PlaylistItem
            viewModel.togglePlaylist(item.playlist)
            binding.save.isEnabled = viewModel.selectedPlaylists.isNotEmpty()
        }

        observe(viewModel.playlists) { list ->
            list ?: return@observe
            val visible = viewModel.playlists.value != null || viewModel.saving
            binding.loading.root.isVisible = !visible
            binding.nestedScrollView.isVisible = visible

            adapter.setItems(
                list.map { it.toMediaItem() },
                viewModel.selectedPlaylists.map { it.toMediaItem() }
            )
        }

        observe(viewModel.dismiss) { dismiss() }

        binding.recyclerView.adapter = adapter
        (binding.recyclerView.layoutManager as GridLayoutManager).spanCount =
            mediaItemSpanCount(requireContext())
        viewModel.clientId = clientId
        viewModel.onInitialize()
    }
}