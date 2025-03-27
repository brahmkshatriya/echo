package dev.brahmkshatriya.echo.ui.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.DialogPlaylistCreateBinding
import dev.brahmkshatriya.echo.ui.main.library.LibraryViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class CreatePlaylistBottomSheet : BottomSheetDialogFragment() {

    var binding by autoCleared<DialogPlaylistCreateBinding>()
    val viewModel by activityViewModel<LibraryViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogPlaylistCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.playlistName.setOnEditorActionListener { _, _, _ ->
            binding.playlistDesc.requestFocus()
            true
        }

        binding.playlistDesc.setOnEditorActionListener { _, _, _ ->
            createPlaylist()
            true
        }

        binding.playlistCreateButton.setOnClickListener { createPlaylist() }
        binding.topAppBar.setNavigationOnClickListener { dismiss() }

        observe(viewModel.createPlaylistStateFlow) {
            when (it) {
                LibraryViewModel.State.CreatePlaylist -> {
                    binding.nestedScrollView.isVisible = true
                    binding.saving.root.isVisible = false
                }

                LibraryViewModel.State.Creating -> {
                    binding.nestedScrollView.isVisible = false
                    binding.saving.root.isVisible = true
                    binding.saving.textView.text =
                        getString(R.string.creating_x, binding.playlistName.text)
                }

                is LibraryViewModel.State.PlaylistCreated -> {
                    if (it.playlist != null) parentFragmentManager.setFragmentResult(
                        "createPlaylist",
                        Bundle().apply {
                            putString("extensionId", it.extensionId)
                            putSerialized("playlist", it.playlist)
                        }
                    )
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
            binding.playlistName.requestFocus()
            return
        }
        val desc = binding.playlistDesc.text.toString().takeIf { it.isNotBlank() }
        viewModel.createPlaylist(title, desc)
    }
}