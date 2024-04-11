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
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.ui.media.MediaClickListener
import dev.brahmkshatriya.echo.ui.media.MediaContainerAdapter
import dev.brahmkshatriya.echo.ui.media.MediaContainerLoadingAdapter.Companion.withLoaders
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
        val adapter = MediaContainerAdapter(this, "track_details", mediaClickListener)

        binding.root.adapter = adapter.withLoaders()
        observe(playerViewModel.currentFlow) {
            it?.clientId ?: return@observe
            adapter.clientId = it.clientId
            viewModel.itemsFlow.value = null
            val track = it.loaded
            if (track == null) observe(it.onLoad) { loaded ->
                viewModel.load(it.clientId, loaded)
            }
            else viewModel.load(it.clientId, track)
        }

        observe(viewModel.itemsFlow) {
            adapter.submit(it)
        }
    }
}

@HiltViewModel
class TrackDetailsViewModel @Inject constructor(
    throwableFlow: MutableSharedFlow<Throwable>,
    val extensionListFlow: ExtensionModule.ExtensionListFlow,
) : CatchingViewModel(throwableFlow) {

    private var previous: Track? = null
    val itemsFlow = MutableStateFlow<PagingData<MediaItemsContainer>?>(null)

    fun load(clientId: String, track: Track) {
        val client = extensionListFlow.getClient(clientId) ?: return
        if (client !is TrackClient) return
        if (previous?.id == track.id) return
        viewModelScope.launch {
            previous = track
            client.getMediaItems(track).collectTo(itemsFlow)
        }
    }

}
