package dev.brahmkshatriya.echo.ui.item

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.UserClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Lists.AlbumItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Lists.PlaylistItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Profile.ArtistItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Profile.UserItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.TrackItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.FragmentItemBinding
import dev.brahmkshatriya.echo.ui.media.MediaContainerAdapter
import dev.brahmkshatriya.echo.ui.media.MediaContainerLoadingAdapter.Companion.withLoaders
import dev.brahmkshatriya.echo.ui.media.MediaItemViewHolder.Companion.placeHolder
import dev.brahmkshatriya.echo.utils.Animator.setupTransition
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.collect
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.load
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.loadWith
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.applyAdapter
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets

@AndroidEntryPoint
class ItemFragment : Fragment() {

    companion object {
        fun newInstance(clientId: String, item: EchoMediaItem) = ItemFragment().apply {
            arguments = Bundle().apply {
                putString("clientId", clientId)
                putParcelable("item", item)
            }
        }
    }

    private val args by lazy { requireArguments() }
    private val clientId by lazy {
        args.getString("clientId")!!
    }

    @Suppress("DEPRECATION")
    private val item by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            args.getParcelable("item", EchoMediaItem::class.java)!!
        else args.getParcelable("item")!!
    }

    private var binding by autoCleared<FragmentItemBinding>()
    private val viewModel by viewModels<ItemViewModel>()
    private val playerVM by activityViewModels<PlayerViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        setupTransition(view)
        applyInsets {
            binding.coverContainer.updateLayoutParams<MarginLayoutParams> { topMargin = it.top }
            binding.recyclerView.applyContentInsets(it)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.appbarOutline.alpha = offset
        }

        binding.toolBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.toolBar.title = item.title.trim()
        binding.endIcon.load(item.placeHolder())
        item.cover.loadInto(binding.cover, item.placeHolder())
        if (item is EchoMediaItem.Profile) binding.coverContainer.run {
            val maxWidth = 240.dpToPx(context)
            radius = maxWidth.toFloat()
            updateLayoutParams<ConstraintLayout.LayoutParams> {
                matchConstraintMaxWidth = maxWidth
            }
        }

        var isRadioClient = false
        var isUserClient = false

        val mediaContainerAdapter = MediaContainerAdapter(this, view.transitionName)

        val trackAdapter = TrackAdapter(
            view.transitionName,
            object : TrackAdapter.Listener {
                override fun onClick(list: List<Track>, position: Int, view: View) {
                    playerVM.play(clientId, list, position)
                }

                override fun onLongClick(list: List<Track>, position: Int, view: View): Boolean {
                    val track = list[position]
                    return mediaContainerAdapter.listener
                        .onLongClick(clientId, track.toMediaItem(), view)
                }
            }
        )

        val albumHeaderAdapter = AlbumHeaderAdapter(
            object : AlbumHeaderAdapter.Listener {
                override fun onPlayClicked(album: Album) = playerVM.play(clientId, album.tracks, 0)
                override fun onRadioClicked(album: Album) = playerVM.radio(clientId, album)
            }
        )

        val playlistHeaderAdapter = PlaylistHeaderAdapter(
            object : PlaylistHeaderAdapter.Listener {
                override fun onPlayClicked(list: Playlist) = playerVM.play(clientId, list.tracks, 0)
                override fun onRadioClicked(list: Playlist) = playerVM.radio(clientId, list)
            }
        )

        val artistHeaderAdapter = ArtistHeaderAdapter(object : ArtistHeaderAdapter.Listener {
            override fun onSubscribeClicked(
                artist: Artist, subscribe: Boolean, adapter: ArtistHeaderAdapter
            ) = Unit

            override fun onRadioClicked(artist: Artist) = playerVM.radio(clientId, artist)
        })

        fun concatAdapter(item: EchoMediaItem): ConcatAdapter {
            return when (item) {
                is AlbumItem ->
                    ConcatAdapter(albumHeaderAdapter, trackAdapter, mediaContainerAdapter)

                is PlaylistItem ->
                    ConcatAdapter(playlistHeaderAdapter, trackAdapter, mediaContainerAdapter)

                is ArtistItem -> ConcatAdapter(artistHeaderAdapter, mediaContainerAdapter)
                else -> mediaContainerAdapter.withLoaders()
            }
        }

        collect(viewModel.extensionListFlow.flow) { list ->
            val client = list?.find { it.metadata.id == clientId }

            mediaContainerAdapter.clientId = clientId
            isRadioClient = client is RadioClient
            isUserClient = client is UserClient

            val item = item
            viewModel.item = item
            viewModel.client = client
            viewModel.initialize()

            binding.recyclerView.run {
                val adapter = concatAdapter(item)
                when (item) {
                    is UserItem -> applyAdapter<UserClient>(client, R.string.user, adapter)
                    is ArtistItem -> applyAdapter<ArtistClient>(client, R.string.artist, adapter)
                    is TrackItem -> applyAdapter<TrackClient>(client, R.string.track, adapter)
                    is AlbumItem -> applyAdapter<AlbumClient>(client, R.string.album, adapter)
                    is PlaylistItem ->
                        applyAdapter<PlaylistClient>(client, R.string.playlist, adapter)
                }
            }
        }

        observe(viewModel.itemFlow) {
            it ?: return@observe
            it.cover.loadWith(binding.cover, item.cover, it.placeHolder())
            when (it) {
                is AlbumItem -> {
                    albumHeaderAdapter.submit(it.album, isRadioClient)
                    trackAdapter.submitList(it.album.tracks)
                }

                is PlaylistItem -> {
                    playlistHeaderAdapter.submit(it.playlist, isRadioClient)
                    trackAdapter.submitList(it.playlist.tracks)
                }

                is ArtistItem ->
                    artistHeaderAdapter.submit(it.artist, isUserClient, isRadioClient)

                else -> Unit
            }
        }

        observe(viewModel.relatedFeed) {
            mediaContainerAdapter.submit(it)
        }
    }
}
