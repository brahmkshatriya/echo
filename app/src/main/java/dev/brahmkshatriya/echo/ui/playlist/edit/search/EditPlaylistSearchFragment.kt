package dev.brahmkshatriya.echo.ui.playlist.edit.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.databinding.FragmentPlaylistSearchBinding
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.main.search.SearchFragment
import dev.brahmkshatriya.echo.ui.media.adapter.MediaItemSelectableAdapter
import dev.brahmkshatriya.echo.ui.media.adapter.MediaItemSelectableAdapter.Companion.mediaItemSpanCount
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx

class EditPlaylistSearchFragment : Fragment() {
    companion object {
        fun getBundle(extensionId: String) = Bundle().apply {
            putString("extensionId", extensionId)
        }
    }

    private var binding by autoCleared<FragmentPlaylistSearchBinding>()
    private val viewModel by viewModels<EditPlaylistSearchViewModel>()

    private val args by lazy { requireArguments() }
    private val extensionId by lazy { args.getString("extensionId")!! }

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

        val searchFragment = binding.playlistSearchContainer.getFragment<SearchFragment>()
        searchFragment.arguments = Bundle().apply {
            putString("extensionId", extensionId)
            putString("itemListener", "search")
        }
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
        binding.recyclerView.mediaItemSpanCount {
            (binding.recyclerView.layoutManager as GridLayoutManager).spanCount = it
        }

        binding.addTracks.setOnClickListener {
            parentFragmentManager.setFragmentResult("searchedTracks", Bundle().apply {
                putSerialized("tracks", viewModel.selectedTracks.value)
            })
            viewModel.selectedTracks.value = emptyList()
            parentFragmentManager.popBackStack()
        }

        observe(viewModel.selectedTracks) { list ->
            val items = list.map {
                it.toMediaItem() to (it in viewModel.selectedTracks.value)
            }
            adapter.submitList(items)
            binding.addTracks.isEnabled = items.isNotEmpty()
            val tracks = items.size
            binding.selectedSongs.text = runCatching {
                resources.getQuantityString(R.plurals.n_songs, tracks, tracks)
            }.getOrNull() ?: getString(R.string.x_songs, tracks)
        }
    }
}