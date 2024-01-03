package dev.brahmkshatriya.echo.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.brahmkshatriya.echo.databinding.FragmentLibraryBinding
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.utils.autoCleared
import dev.brahmkshatriya.echo.ui.utils.updateBottomMarginWithSystemInsets

class LibraryFragment : Fragment() {

    private var binding: FragmentLibraryBinding by autoCleared()

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = FragmentLibraryBinding.inflate(inflater, parent, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        PlayerViewModel.handleBackPress(this)
        updateBottomMarginWithSystemInsets(binding.root)

    }
}