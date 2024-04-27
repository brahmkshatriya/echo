package dev.brahmkshatriya.echo.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentCreatePlaylistBinding
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.setupTransition
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets

class CreatePlaylistFragment : Fragment() {

    var binding by autoCleared<FragmentCreatePlaylistBinding>()
    val viewModel by activityViewModels<LibraryViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreatePlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsets { binding.root.applyInsets(it) }
        applyBackPressCallback()

        binding.playlistName.setOnEditorActionListener { _, _, _ ->
            createPlaylist()
            false
        }

        binding.playlistCreateButton.setOnClickListener { createPlaylist() }
        binding.playlistCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun createPlaylist() {
        val title = binding.playlistName.text.toString()
        if (title.isEmpty()) {
            binding.playlistName.error = getString(R.string.playlist_name_empty)
            return
        }
        viewModel.createPlaylist(title)
        parentFragmentManager.popBackStack()
    }

    override fun onDestroy() {
        super.onDestroy()
        //TODO Shift to Fragment Result
    }
}