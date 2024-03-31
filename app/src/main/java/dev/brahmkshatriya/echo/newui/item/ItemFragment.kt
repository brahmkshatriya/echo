package dev.brahmkshatriya.echo.newui.item

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.ConcatAdapter
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.UserClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Lists
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Profile
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.TrackItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.databinding.FragmentItemBinding
import dev.brahmkshatriya.echo.di.ExtensionModule
import dev.brahmkshatriya.echo.newui.media.MediaContainerAdapter
import dev.brahmkshatriya.echo.newui.media.MediaContainerLoadingAdapter.Companion.withLoaders
import dev.brahmkshatriya.echo.newui.media.MediaItemViewHolder.Companion.placeHolder
import dev.brahmkshatriya.echo.ui.album.AlbumHeaderAdapter
import dev.brahmkshatriya.echo.ui.album.albumImage
import dev.brahmkshatriya.echo.ui.artist.ArtistHeaderAdapter
import dev.brahmkshatriya.echo.ui.playlist.PlaylistHeaderAdapter
import dev.brahmkshatriya.echo.utils.Animator.setupTransition
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.load
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.loadWith
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.getAdapterForExtension
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@AndroidEntryPoint
class ItemFragment : Fragment() {

    @Inject
    lateinit var extensionListFlow: ExtensionModule.ExtensionListFlow

    @Inject
    lateinit var throwableFlow: MutableSharedFlow<Throwable>

    private var binding by autoCleared<FragmentItemBinding>()
    private val args by navArgs<ItemFragmentArgs>()
    private val clientId get() = args.clientId

    private val viewModel by viewModels<ItemViewModel>()
    private val playerVM by activityViewModels<PlayerViewModel>()

    private val mediaContainerAdapter = MediaContainerAdapter(this)

    private val albumHeaderAdapter = AlbumHeaderAdapter(
        object : AlbumHeaderAdapter.Listener {
            override fun onPlayClicked(album: Album) = playerVM.play(clientId, album.tracks)
            override fun onRadioClicked(album: Album) = playerVM.radio(clientId, album)
        }
    )

    private val playlistHeaderAdapter =
        PlaylistHeaderAdapter(object : PlaylistHeaderAdapter.Listener {
            override fun onPlayClicked(playlist: Playlist) =
                playerVM.play(clientId, playlist.tracks)

            override fun onRadioClicked(playlist: Playlist) = playerVM.radio(clientId, playlist)
        })
    private val artistHeaderAdapter = ArtistHeaderAdapter(object : ArtistHeaderAdapter.Listener {
        override fun onSubscribeClicked(
            artist: Artist, subscribe: Boolean, adapter: ArtistHeaderAdapter
        ) = Unit

        override fun onRadioClicked(artist: Artist) = playerVM.radio(clientId, artist)
    })

    private fun concatAdapter(item: EchoMediaItem): ConcatAdapter {
        return when (item) {
            is Lists.AlbumItem -> ConcatAdapter(albumHeaderAdapter, mediaContainerAdapter)
            is Lists.PlaylistItem -> ConcatAdapter(playlistHeaderAdapter, mediaContainerAdapter)
            is Profile.ArtistItem -> ConcatAdapter(artistHeaderAdapter, mediaContainerAdapter)
            else -> mediaContainerAdapter.withLoaders()
        }
    }

    private inline fun <reified T> adapter(
        client: ExtensionClient,
        item: EchoMediaItem,
        string: Int
    ) = getAdapterForExtension<T>(client, string, concatAdapter(item))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTransition(view)
        applyInsets {
            binding.coverContainer.updatePadding(top = it.top)
            binding.recyclerView.updatePadding(bottom = it.bottom)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.appbarOutline.alpha = offset
        }

        binding.toolBar.setupWithNavController(findNavController())

        val transitionName = args.item.id
        binding.root.transitionName = transitionName


        binding.toolBar.title = args.item.title
        binding.endIcon.load(args.item.placeHolder())
        when (args.item) {
            is Profile -> {
                binding.mainCover.isVisible = false
                binding.profileCover.isVisible = true
                args.item.cover.loadInto(binding.profileCover, args.item.placeHolder())
            }

            is Lists -> {
                albumImage((args.item as Lists).size, binding.cover1, binding.cover2)
                binding.mainCover.isVisible = true
                binding.profileCover.isVisible = false
                args.item.cover.loadWith(binding.mainCover, args.item.placeHolder()) {
                    binding.cover1.load(it)
                    binding.cover2.load(it)
                }
            }

            is TrackItem -> {
                albumImage(1, binding.cover1, binding.cover2)
                binding.mainCover.isVisible = true
                binding.profileCover.isVisible = false
                args.item.cover.loadInto(binding.mainCover, args.item.placeHolder())
            }
        }

        var isRadioClient = false
        var isUserClient = false
        observe(extensionListFlow.flow) { list ->
            val client = list?.find { it.metadata.id == args.clientId } ?: return@observe

            mediaContainerAdapter.clientId = args.clientId
            isRadioClient = client is RadioClient
            isUserClient = client is UserClient

            val item = args.item
            viewModel.loadItem(throwableFlow, client, item)
            binding.recyclerView.adapter = when (args.item) {
                is Lists.AlbumItem -> adapter<AlbumClient>(client, item, R.string.album)
                is Lists.PlaylistItem -> adapter<PlaylistClient>(client, item, R.string.playlist)
                is Profile.ArtistItem -> adapter<ArtistClient>(client, item, R.string.artist)
                is Profile.UserItem -> adapter<UserClient>(client, item, R.string.user)
                is TrackItem -> adapter<TrackClient>(client, item, R.string.track)
            }
        }

        observe(viewModel.itemFlow) { item ->
            when (item) {
                is Lists.AlbumItem ->
                    albumHeaderAdapter.submit(item.album, isRadioClient)

                is Lists.PlaylistItem ->
                    playlistHeaderAdapter.submit(item.playlist, isRadioClient)

                is Profile.ArtistItem ->
                    artistHeaderAdapter.submit(item.artist, isUserClient, isRadioClient)

                else -> Unit
            }
        }

        observe(viewModel.relatedFeed) {
            mediaContainerAdapter.submit(it)
        }
    }
}
