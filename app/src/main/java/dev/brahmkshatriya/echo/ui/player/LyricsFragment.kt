package dev.brahmkshatriya.echo.ui.player

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dev.brahmkshatriya.echo.databinding.FragmentLyricsBinding
import dev.brahmkshatriya.echo.utils.autoCleared

class LyricsFragment : Fragment() {

    var binding by autoCleared<FragmentLyricsBinding>()
    val viewModel by activityViewModels<LyricsViewModel>()
}