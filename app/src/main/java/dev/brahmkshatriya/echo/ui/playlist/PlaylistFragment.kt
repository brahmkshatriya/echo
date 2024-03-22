package dev.brahmkshatriya.echo.ui.playlist

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
import com.google.android.material.transition.platform.MaterialFade
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.FragmentAlbumBinding
import dev.brahmkshatriya.echo.player.PlayerViewModel
import dev.brahmkshatriya.echo.player.ui.PlayerBackButtonHelper
import dev.brahmkshatriya.echo.ui.ClickListener
import dev.brahmkshatriya.echo.ui.MediaItemClickListener
import dev.brahmkshatriya.echo.ui.adapters.MediaItemsContainerAdapter
import dev.brahmkshatriya.echo.ui.adapters.TrackAdapter
import dev.brahmkshatriya.echo.ui.extension.ExtensionViewModel
import dev.brahmkshatriya.echo.ui.extension.getAdapterForExtension
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.loadWith
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.updatePaddingWithPlayerAndSystemInsets
import kotlinx.coroutines.flow.combine

class PlaylistFragment : Fragment() {

    private val args: PlaylistFragmentArgs by navArgs()

    private var binding: FragmentAlbumBinding by autoCleared()

    private val viewModel: PlaylistViewModel by viewModels()
    private val extensionViewModel: ExtensionViewModel by activityViewModels()
    private val playerViewModel: PlayerViewModel by activityViewModels()

    private val clickListener = MediaItemClickListener(this)
    private val trackAdapter = TrackAdapter(true, object : ClickListener<Pair<List<Track>, Int>> {
        override fun onClick(item: Pair<List<Track>, Int>) {
            playerViewModel.play(item.first, item.second)
        }

        override fun onLongClick(item: Pair<List<Track>, Int>) {
            val track = item.first[item.second]
            playerViewModel.addToQueue(track)
        }
    })
    private val mediaItemsContainerAdapter = MediaItemsContainerAdapter(this, clickListener)
    private val header =
        PlaylistHeaderAdapter(object : PlaylistHeaderAdapter.PlaylistHeaderListener {
            override fun onPlayClicked(playlist: Playlist) {
                playerViewModel.play(playlist.tracks, 0)
            }

            override fun onRadioClicked(playlist: Playlist) {
                playerViewModel.radio(playlist)
            }
        })

    private val concatAdapter =
        ConcatAdapter(header, trackAdapter, mediaItemsContainerAdapter.withLoadingFooter())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAlbumBinding.inflate(inflater, container, false)
//        enterTransition = MaterialFade()
//        exitTransition = MaterialFade()
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

        val playlist = args.playlist

        binding.root.transitionName = playlist.id
        sharedElementEnterTransition = MaterialContainerTransform(requireContext(), true).apply {
            drawingViewId = R.id.nav_host_fragment
        }
        binding.toolbar.title = playlist.title.trim()

        playlist.cover.loadWith(binding.albumCover, R.drawable.art_album, null) {
            playlist.cover.loadInto(binding.albumCover1, R.drawable.art_album)
            playlist.cover.loadInto(binding.albumCover2, R.drawable.art_album)
        }

        observe(extensionViewModel.extensionFlow) {
            binding.recyclerView.adapter = getAdapterForExtension<PlaylistClient>(
                it, R.string.playlist, concatAdapter, true
            ) { client ->
                if (client == null) return@getAdapterForExtension
                viewModel.loadAlbum(client, extensionViewModel.throwableFlow, playlist)
            }
        }
        val headerFlow = viewModel.playlistFlow
            .combine(extensionViewModel.extensionFlow) { it, client -> it to client }
        observe(headerFlow) { (it, client) ->
            if (it != null) {
                trackAdapter.submitList(it.tracks)
                header.submit(it, client is RadioClient)
            }
        }
        observe(viewModel.result) {
            if (it != null) mediaItemsContainerAdapter.submit(it)
        }
    }
}