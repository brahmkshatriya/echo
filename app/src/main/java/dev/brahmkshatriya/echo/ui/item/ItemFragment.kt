package dev.brahmkshatriya.echo.ui.item

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.ArtistFollowClient
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
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Lists.RadioItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Profile.ArtistItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.databinding.FragmentItemBinding
import dev.brahmkshatriya.echo.extensions.isClient
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.icon
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.placeHolder
import dev.brahmkshatriya.echo.ui.adapter.ShelfAdapter
import dev.brahmkshatriya.echo.ui.adapter.ShelfAdapter.Companion.getListener
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.editplaylist.EditPlaylistFragment
import dev.brahmkshatriya.echo.utils.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.collect
import dev.brahmkshatriya.echo.utils.configure
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.getSerialized
import dev.brahmkshatriya.echo.utils.load
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.loadWithThumb
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.putSerialized
import dev.brahmkshatriya.echo.utils.setupTransition
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.applyAdapter
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyFabInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ItemFragment : Fragment() {

    companion object {
        fun newInstance(clientId: String, item: EchoMediaItem) = ItemFragment().apply {
            arguments = Bundle().apply {
                putString("clientId", clientId)
                putSerialized("item", item)
            }
        }
    }

    private val args by lazy { requireArguments() }
    private val clientId by lazy { args.getString("clientId")!! }
    private val item by lazy { args.getSerialized<EchoMediaItem>("item")!! }

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

    private var shelfAdapter: ShelfAdapter? = null
    private var trackAdapter: TrackAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        setupTransition(view)
        applyInsets {
            binding.coverContainer.updateLayoutParams<MarginLayoutParams> { topMargin = it.top }
            binding.recyclerView.applyContentInsets(it)
            binding.fabContainer.applyFabInsets(it, systemInsets.value)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.appbarOutline.alpha = offset
        }

        binding.toolBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_more -> {
                    val loaded = viewModel.itemFlow.value
                    ItemBottomSheet.newInstance(
                        clientId,
                        loaded ?: item,
                        loaded = loaded != null,
                        fromPlayer = false
                    ).show(parentFragmentManager, null)
                    true
                }

                else -> false
            }
        }

        FastScrollerHelper.applyTo(binding.recyclerView)
        binding.toolBar.title = item.title.trim().takeIf { it.isNotEmpty() }
        binding.endIcon.load(item.icon())
        item.cover.loadInto(binding.cover, item.placeHolder())
        if (item is EchoMediaItem.Profile) binding.coverContainer.run {
            val maxWidth = 240.dpToPx(context)
            radius = maxWidth.toFloat()
            updateLayoutParams<ConstraintLayout.LayoutParams> {
                matchConstraintMaxWidth = maxWidth
            }
        }

        val listener = getListener(this)

        val albumHeaderAdapter = AlbumHeaderAdapter(
            object : AlbumHeaderAdapter.Listener {
                override fun onPlayClicked(album: Album) =
                    playerVM.play(clientId, album.toMediaItem(), 0)

                override fun onRadioClicked(album: Album) =
                    playerVM.radio(clientId, album.toMediaItem())
            }
        )

        val playlistHeaderAdapter = PlaylistHeaderAdapter(
            object : PlaylistHeaderAdapter.Listener {
                override fun onPlayClicked(list: Playlist) =
                    playerVM.play(clientId, list.toMediaItem(), 0)

                override fun onRadioClicked(list: Playlist) =
                    playerVM.radio(clientId, list.toMediaItem())
            }
        )

        val artistHeaderAdapter = ArtistHeaderAdapter(object : ArtistHeaderAdapter.Listener {
            override fun onSubscribeClicked(artist: Artist, subscribe: Boolean) =
                viewModel.subscribe(artist, subscribe)

            override fun onRadioClicked(artist: Artist) =
                playerVM.radio(clientId, artist.toMediaItem())
        })

        fun concatAdapter(item: EchoMediaItem, itemsAdapter: ConcatAdapter): ConcatAdapter {
            trackAdapter = TrackAdapter(clientId, view.transitionName, listener, item, true)
            return when (item) {
                is AlbumItem ->
                    ConcatAdapter(albumHeaderAdapter, trackAdapter, itemsAdapter)

                is PlaylistItem ->
                    ConcatAdapter(playlistHeaderAdapter, trackAdapter, itemsAdapter)

                is RadioItem -> ConcatAdapter(trackAdapter)

                is ArtistItem -> ConcatAdapter(artistHeaderAdapter, itemsAdapter)
                else -> itemsAdapter
            }
        }

        binding.swipeRefresh.configure {
            viewModel.load()
        }

        viewModel.songsLiveData.observe(viewLifecycleOwner) { songs ->
            lifecycleScope.launch {
                trackAdapter?.submit(songs)
            }
        }

        parentFragmentManager.setFragmentResultListener("reload", this) { _, bundle ->
            if (bundle.getString("id") == viewModel.itemFlow.value?.id)
                viewModel.load()
        }

        parentFragmentManager.setFragmentResultListener("deleted", this) { _, bundle ->
            if (bundle.getString("id") == viewModel.itemFlow.value?.id)
                parentFragmentManager.popBackStack()
        }


        collect(viewModel.relatedFeed) {
            shelfAdapter?.submit(it)
        }

        collect(viewModel.itemFlow) {
            binding.swipeRefresh.isRefreshing = it == null
            it ?: return@collect

            //I have no idea why this doesn't update, if title already exists
            binding.toolBar.post {
                binding.toolBar.title = it.title.trim()
            }

            it.cover.loadWithThumb(binding.cover, item.cover, it.placeHolder())
            with(viewModel) {
                when (it) {
                    is AlbumItem -> {
                        albumHeaderAdapter.submit(it.album, isRadioClient)
                        loadAlbumTracks(it.album)
                    }

                    is PlaylistItem -> {
                        playlistHeaderAdapter.submit(it.playlist, isRadioClient)
                        loadPlaylistTracks(it.playlist)
                    }

                    is RadioItem -> {
                        loadRadioTracks(it.radio)
                    }

                    is ArtistItem ->
                        artistHeaderAdapter.submit(it.artist, isFollowClient, isRadioClient)

                    else -> Unit
                }
            }

            binding.fabContainer.isVisible = if (it is PlaylistItem && it.playlist.isEditable) {
                binding.fabEditPlaylist.transitionName = "edit${it.playlist.id}"
                binding.fabEditPlaylist.setOnClickListener { view1 ->
                    openFragment(
                        EditPlaylistFragment.newInstance(clientId, it.playlist), view1
                    )
                }
                true
            } else false
        }


        collect(viewModel.extensionListFlow) { list ->
            val extension = list?.find { it.metadata.id == clientId }
            extension ?: return@collect

            val mediaAdapter =
                ShelfAdapter(this, view.transitionName, extension, listener)
            shelfAdapter = mediaAdapter
            viewModel.isRadioClient = extension.isClient<RadioClient>()
            viewModel.isFollowClient = extension.isClient<ArtistFollowClient>()

            val item = item
            viewModel.item = item
            viewModel.extension = extension
            viewModel.initialize()

            binding.recyclerView.run {
                val adapter = concatAdapter(item, mediaAdapter.withLoaders())
                when (item) {
                    is EchoMediaItem.Profile.UserItem ->
                        applyAdapter<UserClient>(extension, R.string.user, adapter)

                    is ArtistItem ->
                        applyAdapter<ArtistClient>(extension, R.string.artist, adapter)

                    is EchoMediaItem.TrackItem ->
                        applyAdapter<TrackClient>(extension, R.string.track, adapter)

                    is AlbumItem ->
                        applyAdapter<AlbumClient>(extension, R.string.album, adapter)

                    is PlaylistItem ->
                        applyAdapter<PlaylistClient>(extension, R.string.playlist, adapter)

                    is RadioItem ->
                        applyAdapter<RadioClient>(extension, R.string.radio, adapter)
                }
            }
        }

    }
}
