package dev.brahmkshatriya.echo.ui.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.databinding.FragmentPlaylistSearchBinding
import dev.brahmkshatriya.echo.ui.media.MediaItemSelectableAdapter
import dev.brahmkshatriya.echo.ui.search.SearchFragment
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.setupTransition
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets

@AndroidEntryPoint
class SearchForPlaylistFragment : Fragment() {
    private var binding by autoCleared<FragmentPlaylistSearchBinding>()
    private val viewModel by activityViewModels<SearchForPlaylistViewModel>()

    private val args by lazy { requireArguments() }
    private val clientId by lazy { args.getString("clientId")!! }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaylistSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)

        val behavior = BottomSheetBehavior.from(binding.bottomSheet)
        binding.bottomSheetDragHandle.setOnClickListener { behavior.state = STATE_EXPANDED }
        var topInset = 0
        applyInsets {
            topInset = it.top
            behavior.peekHeight = 72.dpToPx(requireContext()) + it.bottom
            binding.playlistSearchContainer.updatePadding(bottom = it.bottom)
            binding.recyclerView.updatePadding(top = it.top)
        }

        val backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackStarted(backEvent: BackEventCompat) =
                behavior.startBackProgress(backEvent)

            override fun handleOnBackProgressed(backEvent: BackEventCompat) =
                behavior.updateBackProgress(backEvent)

            override fun handleOnBackPressed() = behavior.handleBackInvoked()
            override fun handleOnBackCancelled() = behavior.cancelBackProgress()
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(p0: View, p1: Int) {
                backCallback.isEnabled = p1 == STATE_EXPANDED
            }

            override fun onSlide(p0: View, p1: Float) {
                binding.selectedSongsLayout.translationY = p1 * topInset
                binding.recyclerView.alpha = p1
                binding.bottomSheetDragHandle.alpha = 1 - p1
            }
        })
        applyBackPressCallback()

        val searchFragment = binding.playlistSearchContainer.getFragment<SearchFragment>()
        searchFragment.arguments = Bundle().apply { putString("clientId", clientId) }

        args.putString("itemListener", "search")
        searchFragment.parentFragmentManager.addFragmentOnAttachListener { _, fragment ->
            val arguments = fragment.arguments ?: Bundle()
            arguments.putString("itemListener", "search")
            fragment.arguments = arguments
        }

        val adapter = MediaItemSelectableAdapter { _, item ->
            item as EchoMediaItem.TrackItem
            viewModel.toggleTrack(item.track)
        }

        binding.recyclerView.adapter = adapter
        (binding.recyclerView.layoutManager as GridLayoutManager).spanCount =
            MediaItemSelectableAdapter.mediaItemSpanCount(requireContext())

        binding.addTracks.setOnClickListener {
            parentFragmentManager.setFragmentResult("searchedTracks", Bundle().apply {
                putParcelableArray("tracks", viewModel.selectedTracks.value.toTypedArray())
            })
            viewModel.selectedTracks.value = emptyList()
            parentFragmentManager.popBackStack()
        }

        observe(viewModel.selectedTracks) { list ->
            adapter.setItems(
                list.map { it.toMediaItem() },
                viewModel.selectedTracks.value.map { it.toMediaItem() }
            )
            binding.addTracks.isEnabled = list.isNotEmpty()
            binding.selectedSongs.text = getString(R.string.selected_songs, list.size)
        }
    }

    companion object {
        fun newInstance(clientId: String, playlist: Playlist) = SearchForPlaylistFragment().apply {
            arguments = Bundle().apply {
                putString("clientId", clientId)
                putParcelable("playlist", playlist)
            }
        }
    }
}

