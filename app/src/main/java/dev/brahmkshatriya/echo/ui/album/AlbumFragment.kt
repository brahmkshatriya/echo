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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.platform.MaterialContainerTransform
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.FragmentAlbumBinding
import dev.brahmkshatriya.echo.player.PlayerViewModel
import dev.brahmkshatriya.echo.player.ui.PlayerBackButtonHelper
import dev.brahmkshatriya.echo.ui.ClickListener
import dev.brahmkshatriya.echo.ui.adapters.TrackAdapter
import dev.brahmkshatriya.echo.ui.extension.ExtensionViewModel
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.loadWith

class AlbumFragment : Fragment() {

    private val args: AlbumFragmentArgs by navArgs()

    private var binding: FragmentAlbumBinding by autoCleared()

    private val viewModel: AlbumViewModel by viewModels()
    private val extensionViewModel: ExtensionViewModel by activityViewModels()
    private val playerViewModel: PlayerViewModel by activityViewModels()

//    private val clickListener = MediaItemClickListener(this)
    private val trackAdapter = TrackAdapter(false, object : ClickListener<Pair<List<Track>, Int>> {

        override fun onClick(item: Pair<List<Track>, Int>) {
            playerViewModel.play(item.first, item.second)
        }

        override fun onLongClick(item: Pair<List<Track>, Int>) {
            val track = item.first[item.second]
            playerViewModel.addToQueue(track)
        }

    })
//    private val mediaItemsContainerAdapter = MediaItemsContainerAdapter(this, clickListener)

//    private val concatAdapter =
//        ConcatAdapter(trackAdapter, mediaItemsContainerAdapter.withLoadingFooter())

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
//            binding.recyclerView.updatePaddingWithPlayerAndSystemInsets(it, false)
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

        val album: Album = args.album

        binding.root.transitionName = album.id
        sharedElementEnterTransition = MaterialContainerTransform(requireContext(), true).apply {
            drawingViewId = R.id.nav_host_fragment
        }
        binding.toolbar.title = album.title.trim()

        album.numberOfTracks.let {
            albumImage(it, binding.albumCover1, binding.albumCover2)
            album.cover.loadWith(binding.albumCover, R.drawable.art_album, null) {
                album.cover.loadInto(binding.albumCover1, R.drawable.art_album)
                album.cover.loadInto(binding.albumCover2, R.drawable.art_album)
            }
        }

//        observe(extensionViewModel.extensionFlow) {
//            binding.recyclerView.adapter = getAdapterForExtension<AlbumClient>(
//                it, R.string.album, concatAdapter
//            ) { client ->
//                if (client == null) return@getAdapterForExtension
//                viewModel.loadAlbum(client, extensionViewModel.throwableFlow, album)
//            }
//        }
//        val headerFlow =
//            viewModel.albumFlow.combine(extensionViewModel.extensionFlow) { it, client -> it to client }
//        observe(headerFlow) { (it, client) ->
//            if (it != null) {
//                trackAdapter.submitList(it.tracks)
////                header.submit(it, client is RadioClient)
//            }
//        }
//        observe(viewModel.result) {
//            if (it != null) mediaItemsContainerAdapter.submit(it)
//        }
    }
}