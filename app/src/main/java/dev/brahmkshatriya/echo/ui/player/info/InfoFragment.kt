package dev.brahmkshatriya.echo.ui.player.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.brahmkshatriya.echo.databinding.FragmentPlayerInfoBinding
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class InfoFragment : Fragment() {
    private var binding by autoCleared<FragmentPlayerInfoBinding>()
    private val viewModel by activityViewModel<TrackInfoViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayerInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view, false)
        observe(viewModel.currentFlow) { viewModel.load() }
        observe(viewModel.itemsFlow) {
//            mediaAdapter?.submit(it)
        }
    }
}