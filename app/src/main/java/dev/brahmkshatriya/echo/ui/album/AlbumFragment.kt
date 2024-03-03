package dev.brahmkshatriya.echo.ui.album

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialElevationScale
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.databinding.FragmentCollapsingBarBinding
import dev.brahmkshatriya.echo.player.PlayerViewModel
import dev.brahmkshatriya.echo.player.ui.PlayerBackButtonHelper
import dev.brahmkshatriya.echo.ui.MediaItemClickListener
import dev.brahmkshatriya.echo.ui.adapters.MediaItemsContainerAdapter
import dev.brahmkshatriya.echo.ui.adapters.TrackAdapter
import dev.brahmkshatriya.echo.ui.extension.ExtensionViewModel
import dev.brahmkshatriya.echo.ui.extension.getAdapterForExtension
import dev.brahmkshatriya.echo.ui.snackbar.SnackBarViewModel
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.updatePaddingWithPlayerAndSystemInsets

class AlbumFragment : Fragment() {

    private val args: AlbumFragmentArgs by navArgs()

    private var binding: FragmentCollapsingBarBinding by autoCleared()

    private val viewModel: AlbumViewModel by viewModels()
    private val extensionViewModel: ExtensionViewModel by activityViewModels()
    private val snackBarViewModel: SnackBarViewModel by activityViewModels()
    private val playerViewModel: PlayerViewModel by activityViewModels()

    private val clickListener = MediaItemClickListener(this)
    private val trackAdapter = TrackAdapter(clickListener, false)
    private val mediaItemsContainerAdapter = MediaItemsContainerAdapter(this, clickListener)
    private val header = AlbumHeaderAdapter(object : AlbumHeaderAdapter.AlbumHeaderListener {
        override fun onPlayClicked(album: Album.Full) {
            playerViewModel.play(album.tracks)
        }

        override fun onShuffleClicked(album: Album.Full) {
            album.tracks.forEach {
                playerViewModel.addToQueue(it)
            }
        }
    })
    private val concatAdapter = ConcatAdapter(header, trackAdapter, mediaItemsContainerAdapter)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCollapsingBarBinding.inflate(inflater, container, false)
        enterTransition = MaterialElevationScale(true)
        exitTransition = MaterialElevationScale(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PlayerBackButtonHelper.addCallback(this) {
            binding.recyclerView.updatePaddingWithPlayerAndSystemInsets(it, false)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.albumCoverContainer.updatePadding(top = insets.top)
            windowInsets
        }
        binding.appBarLayout.addOnOffsetChangedListener { appbar, verticalOffset ->
            val offset = (-verticalOffset) / appbar.totalScrollRange.toFloat()
            val inverted = 1 - offset
            binding.endIcon.alpha = inverted
            binding.toolbarOutline.alpha = offset
        }
        binding.toolbar.setupWithNavController(findNavController())
        postponeEnterTransition()
        binding.recyclerView.doOnPreDraw {
            startPostponedEnterTransition()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        val album: Album.Small = args.albumWithCover ?: args.albumSmall ?: return

        binding.root.transitionName = album.uri.toString()
        sharedElementEnterTransition = MaterialContainerTransform(requireContext(), true).apply {
            drawingViewId = R.id.nav_host_fragment
        }
        binding.toolbar.title = album.title.trim()

        (album as? Album.WithCover).let {
            it?.numberOfTracks?.let { it1 ->
                albumImage(it1, binding.albumCover1, binding.albumCover2)
                it.cover.loadInto(binding.albumCover1, R.drawable.art_album)
                it.cover.loadInto(binding.albumCover2, R.drawable.art_album)
            }
            it?.cover.loadInto(binding.albumCover, R.drawable.art_album)
        }

        observe(extensionViewModel.extensionFlow) {
            binding.recyclerView.adapter = getAdapterForExtension<AlbumClient>(
                it, R.string.album, concatAdapter, true
            ) { client ->
                if (client == null) return@getAdapterForExtension
                viewModel.loadAlbum(client, snackBarViewModel.mutableExceptionFlow, album)
            }
        }
        observe(viewModel.albumFlow) {
            if (it != null) {
                trackAdapter.submitList(it.tracks)
                header.submit(it)
            }
        }
        observe(viewModel.result) {
            if (it != null) mediaItemsContainerAdapter.submit(it)
        }
    }
}