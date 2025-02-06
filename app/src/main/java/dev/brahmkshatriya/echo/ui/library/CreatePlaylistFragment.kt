package dev.brahmkshatriya.echo.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentCreatePlaylistBinding
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.putSerialized

class CreatePlaylistFragment : BottomSheetDialogFragment() {

    var binding by autoCleared<FragmentCreatePlaylistBinding>()
    val viewModel by activityViewModels<LibraryViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreatePlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.playlistName.setOnEditorActionListener { _, _, _ ->
            createPlaylist()
            false
        }

        binding.playlistCreateButton.setOnClickListener { createPlaylist() }
        binding.playlistCancel.setOnClickListener { dismiss() }

        observe(viewModel.createPlaylistStateFlow) {
            when (it) {
                LibraryViewModel.State.CreatePlaylist -> {
                    binding.nestedScrollView.isVisible = true
                    binding.saving.root.isVisible = false
                }

                LibraryViewModel.State.Creating -> {
                    binding.nestedScrollView.isVisible = false
                    binding.saving.root.isVisible = true
                    binding.saving.textView.setText(R.string.saving)
                }

                is LibraryViewModel.State.PlaylistCreated -> {
                    parentFragmentManager.setFragmentResult("createPlaylist", Bundle().apply {
                        putString("extensionId", it.extensionId)
                        putSerialized("playlist", it.playlist)
                    })
                    viewModel.createPlaylistStateFlow.value = LibraryViewModel.State.CreatePlaylist
                    dismiss()
                }
            }
        }
    }

    private fun createPlaylist() {
        val title = binding.playlistName.text.toString()
        if (title.isEmpty()) {
            binding.playlistName.error = getString(R.string.playlist_name_empty)
            return
        }
        val desc = binding.playlistDesc.text.toString().takeIf { it.isNotBlank() }
        viewModel.createPlaylist(title, desc)
    }
}