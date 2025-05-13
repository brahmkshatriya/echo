package dev.brahmkshatriya.echo.ui.player.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import dev.brahmkshatriya.echo.databinding.FragmentPlayerInfoBinding
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfAdapter.Companion.getShelfAdapter
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfClickListener
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class InfoFragment : Fragment() {
    private var binding by autoCleared<FragmentPlayerInfoBinding>()
    private val viewModel by activityViewModel<TrackInfoViewModel>()

    private val listener by lazy {
        ShelfClickListener(requireActivity().supportFragmentManager)
    }
    private val shelfAdapter by lazy { getShelfAdapter(listener) }
    private val trackInfoAdapter by lazy { TrackInfoAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayerInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view, false)
        observe(viewModel.currentFlow) {
            trackInfoAdapter.submit(it?.mediaItem?.extensionId, it?.track)
            viewModel.load()
        }
        observe(viewModel.itemsFlow) { (ext, _, shelves, item) ->
            shelfAdapter.submit(ext?.id, shelves, item)
        }
        binding.root.adapter = ConcatAdapter(
            trackInfoAdapter,
            shelfAdapter.withLoaders(this)
        )
        shelfAdapter.getTouchHelper().attachToRecyclerView(binding.root)
    }
}