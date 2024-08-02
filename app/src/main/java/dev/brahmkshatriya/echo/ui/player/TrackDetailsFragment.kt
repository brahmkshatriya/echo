package dev.brahmkshatriya.echo.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.FragmentTrackDetailsBinding
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.ui.adapter.MediaClickListener
import dev.brahmkshatriya.echo.ui.adapter.MediaContainerAdapter
import dev.brahmkshatriya.echo.ui.paging.toFlow
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.CatchingViewModel
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class TrackDetailsFragment : Fragment() {
    private var binding by autoCleared<FragmentTrackDetailsBinding>()
    private val playerViewModel by activityViewModels<PlayerViewModel>()
    private val uiViewModel by activityViewModels<UiViewModel>()
    private val viewModel by activityViewModels<TrackDetailsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTrackDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mediaClickListener = MediaClickListener(requireActivity().supportFragmentManager) {
            uiViewModel.collapsePlayer()
        }

        var mediaAdapter: MediaContainerAdapter? = null
        observe(playerViewModel.currentFlow) {
            val (_, item) = it ?: return@observe
            val extension =
                playerViewModel.extensionListFlow.getExtension(item.clientId) ?: return@observe
            val adapter =
                MediaContainerAdapter(this, "track_details", extension.info, mediaClickListener)
            mediaAdapter = adapter
            binding.root.adapter = adapter.withLoaders()
            viewModel.load(item.clientId, item.track)
        }

        observe(viewModel.itemsFlow) {
            mediaAdapter?.submit(it)
        }
    }
}

@HiltViewModel
class TrackDetailsViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
) : CatchingViewModel(throwableFlow) {

    private var previous: Track? = null
    val itemsFlow = MutableStateFlow<PagingData<MediaItemsContainer>?>(null)

    fun load(clientId: String, track: Track) {
        if (previous?.id == track.id) return
        previous = track
        itemsFlow.value = null
        val extension = extensionListFlow.getExtension(clientId) ?: return
        val client = extension.client
        if (client !is TrackClient) return
        viewModelScope.launch {
            tryWith(extension.info) { client.getMediaItems(track).toFlow().collectTo(itemsFlow) }
        }
    }

}
