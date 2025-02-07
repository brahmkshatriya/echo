package dev.brahmkshatriya.echo.ui.item

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.builtin.offline.OfflineExtension
import dev.brahmkshatriya.echo.common.clients.ArtistFollowClient
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.SaveToLibraryClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.clients.TrackHideClient
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.DialogMediaItemBinding
import dev.brahmkshatriya.echo.databinding.ItemDialogButtonBinding
import dev.brahmkshatriya.echo.databinding.ItemDialogButtonLoadingBinding
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.ui.adapter.MediaItemViewHolder.Companion.applyIsPlaying
import dev.brahmkshatriya.echo.ui.adapter.ShelfViewHolder.Media.Companion.bind
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.editplaylist.AddToPlaylistBottomSheet
import dev.brahmkshatriya.echo.ui.exception.ExceptionFragment.Companion.copyToClipboard
import dev.brahmkshatriya.echo.ui.player.AudioEffectsBottomSheet
import dev.brahmkshatriya.echo.ui.player.SleepTimerBottomSheet
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.getSerialized
import dev.brahmkshatriya.echo.utils.image.loadAsCircle
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.putSerialized
import dev.brahmkshatriya.echo.viewmodels.DownloadViewModel
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import dev.brahmkshatriya.echo.viewmodels.UiViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ItemBottomSheet : BottomSheetDialogFragment() {
    companion object {
        fun newInstance(
            clientId: String, item: EchoMediaItem, loaded: Boolean, fromPlayer: Boolean
        ) = ItemBottomSheet().apply {
            arguments = Bundle().apply {
                putString("clientId", clientId)
                putSerialized("item", item)
                putBoolean("loaded", loaded)
                putBoolean("fromPlayer", fromPlayer)
            }
        }
    }

    private var binding by autoCleared<DialogMediaItemBinding>()
    private val viewModel by viewModels<ItemViewModel>()
    private val playerViewModel by activityViewModels<PlayerViewModel>()
    private val downloadViewModel by activityViewModels<DownloadViewModel>()
    private val uiViewModel by activityViewModels<UiViewModel>()

    private val args by lazy { requireArguments() }
    private val clientId by lazy { args.getString("clientId")!! }
    private val item by lazy { args.getSerialized<EchoMediaItem>("item")!! }
    private val fromPlayer by lazy { args.getBoolean("fromPlayer") }
    private val loaded by lazy { args.getBoolean("loaded") }
    private val extension by lazy { playerViewModel.extensionListFlow.getExtension(clientId) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogMediaItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.item = item
        viewModel.extension = extension
        viewModel.loadRelatedFeed = false

        binding.itemContainer.run {
            more.run {
                setOnClickListener { dismiss() }
                setIconResource(R.drawable.ic_close)
                contentDescription = context.getString(R.string.close)
            }
            val isPlaying = bind(item)
            if (!loaded) root.setOnClickListener {
                openItemFragment(item)
                dismiss()
            }
            observe(playerViewModel.currentFlow) {
                applyIsPlaying(it, viewModel.item?.id, isPlaying)
            }
        }

        lifecycleScope.launch {
            val client = extension?.instance?.value()?.getOrNull() ?: return@launch run {
                createSnack(requireContext().noClient())
                dismiss()
            }
            if (!loaded) {
                binding.recyclerView.adapter =
                    ConcatAdapter(ActionAdapter(getActions(client, item, false)), LoadingAdapter())
                viewModel.initialize()
                observe(viewModel.itemFlow) {
                    if (it != null) {
                        binding.itemContainer.bind(it)
                        binding.recyclerView.adapter = ActionAdapter(getActions(client, it, true))
                    }
                }
            } else {
                binding.recyclerView.adapter = ActionAdapter(getActions(client, item, true))
            }
        }
        observe(playerViewModel.shareLink) {
            requireContext().copyToClipboard(it, it)
        }
    }

    private fun openItemFragment(item: EchoMediaItem) {
        requireActivity().openFragment(ItemFragment.newInstance(clientId, item))
        uiViewModel.collapsePlayer()
    }

    private fun getActions(
        client: ExtensionClient, item: EchoMediaItem, loaded: Boolean
    ) = when (item) {
        is EchoMediaItem.Lists -> getListButtons(client, item) + when (item) {
            is EchoMediaItem.Lists.AlbumItem -> listOfNotNull(
                radioButton(client, item, loaded),
                saveToPlaylist(client, item),
                saveToLibraryButton(client, item, loaded),
                downloadButton(client, item),
            ) + item.album.artists.map {
                ItemAction.Custom(it.cover, R.drawable.ic_artist, it.name) {
                    openItemFragment(it.toMediaItem())
                }
            }

            is EchoMediaItem.Lists.PlaylistItem -> listOfNotNull(
                radioButton(client, item, loaded),
                saveToPlaylist(client, item),
                saveToLibraryButton(client, item, loaded),
                downloadButton(client, item),
                if (client is LibraryFeedClient && item.playlist.isEditable)
                    ItemAction.Resource(R.drawable.ic_delete, R.string.delete_playlist) {
                        playerViewModel.deletePlaylist(clientId, item.playlist)
                        parentFragmentManager.setFragmentResult("deleted", Bundle().apply {
                            putString("id", item.id)
                        })
                    }
                else null,
            ) + item.playlist.authors.map {
                ItemAction.Custom(it.cover, R.drawable.ic_artist, it.name) {
                    openItemFragment(it.toMediaItem())
                }
            }

            is EchoMediaItem.Lists.RadioItem ->
                listOfNotNull(saveToLibraryButton(client, item, loaded))
        }

        is EchoMediaItem.Profile -> when (item) {
            is EchoMediaItem.Profile.ArtistItem -> listOfNotNull(
                radioButton(client, item, loaded),
                saveToLibraryButton(client, item, loaded),
                followButton(client, item.artist, loaded)
            )

            is EchoMediaItem.Profile.UserItem -> listOf()
        }

        is EchoMediaItem.TrackItem -> getTrackButtons(client, item.track) + listOfNotNull(
            likeButton(client, item.track, loaded),
            hideButton(client, item.track, loaded),
            radioButton(client, item, loaded),
            saveToPlaylist(client, item),
            saveToLibraryButton(client, item, loaded),
            downloadButton(client, item),
            item.track.album?.let {
                ItemAction.Custom(it.cover, R.drawable.ic_album, it.title) {
                    openItemFragment(it.toMediaItem())
                }
            }
        ) + item.track.artists.map {
            ItemAction.Custom(it.cover, R.drawable.ic_artist, it.name) {
                openItemFragment(it.toMediaItem())
            }
        }

    } + listOfNotNull(shareButton(client, item, loaded))

    private fun getListButtons(
        client: ExtensionClient, item: EchoMediaItem.Lists
    ) = if (client is TrackClient) listOf(
        ItemAction.Resource(R.drawable.ic_play, R.string.play) {
            playerViewModel.play(clientId, item, 0)
        },
        ItemAction.Resource(R.drawable.ic_playlist_play, R.string.play_next) {
            playerViewModel.addToQueue(clientId, item, false)
        },
        ItemAction.Resource(R.drawable.ic_playlist_add, R.string.add_to_queue) {
            playerViewModel.addToQueue(clientId, item, true)
        }
    ) else listOf()

    private fun getTrackButtons(client: ExtensionClient, track: Track) =
        if (client is TrackClient && !fromPlayer) listOfNotNull(
            ItemAction.Resource(R.drawable.ic_play, R.string.play) {
                playerViewModel.play(clientId, track)
            },
            if (playerViewModel.list.isNotEmpty())
                ItemAction.Resource(R.drawable.ic_playlist_play, R.string.play_next) {
                    playerViewModel.addToQueue(clientId, track, false)
                }
            else null,
            if (playerViewModel.list.size > 1)
                ItemAction.Resource(R.drawable.ic_playlist_add, R.string.add_to_queue) {
                    playerViewModel.addToQueue(clientId, track, true)
                }
            else null
        )
        else listOf(
            ItemAction.Resource(R.drawable.ic_equalizer, R.string.audio_fx) {
                AudioEffectsBottomSheet().show(parentFragmentManager, null)
            },
            ItemAction.Resource(R.drawable.ic_snooze, R.string.sleep_timer) {
                SleepTimerBottomSheet().show(parentFragmentManager, null)
            }
        )

    private fun likeButton(
        client: ExtensionClient, track: Track, loaded: Boolean
    ) = if (client is TrackLikeClient && loaded)
        if (!track.isLiked)
            ItemAction.Resource(R.drawable.ic_heart_outline, R.string.like) {
                viewModel.like(track, true)
            }
        else ItemAction.Resource(R.drawable.ic_heart_filled, R.string.unlike) {
            viewModel.like(track, false)
        }
    else null

    private fun hideButton(
        client: ExtensionClient, track: Track, loaded: Boolean
    ) = if (client is TrackHideClient && loaded)
        if (!track.isHidden)
            ItemAction.Resource(R.drawable.ic_hide_outline, R.string.hide) {
                viewModel.hide(track, true)
            }
        else ItemAction.Resource(R.drawable.ic_hide_filled, R.string.unhide) {
            viewModel.hide(track, false)
        }
    else null

    private fun followButton(
        client: ExtensionClient, artist: Artist, loaded: Boolean
    ) = if (client is ArtistFollowClient && loaded)
        if (!artist.isFollowing)
            ItemAction.Resource(R.drawable.ic_heart_outline, R.string.follow) {
                viewModel.subscribe(artist, true)
            }
        else ItemAction.Resource(R.drawable.ic_heart_filled, R.string.unfollow) {
            viewModel.subscribe(artist, false)
        }
    else null

    private fun saveToPlaylist(
        client: ExtensionClient, item: EchoMediaItem
    ) = if (client is LibraryFeedClient)
        ItemAction.Resource(R.drawable.ic_library_music, R.string.save_to_playlist) {
            AddToPlaylistBottomSheet.newInstance(clientId, item)
                .show(parentFragmentManager, null)
        }
    else null

    private fun downloadButton(
        client: ExtensionClient, item: EchoMediaItem
    ) = if (client !is OfflineExtension && client is TrackClient)
        ItemAction.Resource(R.drawable.ic_download_for_offline, R.string.download) {
            downloadViewModel.addToDownload(requireActivity(), clientId, item)
        }
    else null

    private fun radioButton(
        client: ExtensionClient, item: EchoMediaItem, loaded: Boolean
    ) = if (client is RadioClient && loaded) ItemAction.Resource(
        R.drawable.ic_sensors, R.string.radio
    ) { playerViewModel.radio(clientId, item) }
    else null

    private fun shareButton(client: ExtensionClient, item: EchoMediaItem, loaded: Boolean) =
        if (client is ShareClient && loaded) ItemAction.Resource(
            R.drawable.ic_forward,
            R.string.share
        ) {
            val shareClient = client as ShareClient
            playerViewModel.onShare(shareClient, item)
        } else null

    private fun saveToLibraryButton(
        client: ExtensionClient, item: EchoMediaItem, loaded: Boolean
    ) = if (client is SaveToLibraryClient && loaded) {
        if (viewModel.savedState.value) ItemAction.Resource(
            R.drawable.ic_bookmark_filled, R.string.remove_from_library
        ) { viewModel.saveToLibrary(item, false) }
        else ItemAction.Resource(
            R.drawable.ic_bookmark_outline, R.string.save_to_library
        ) { viewModel.saveToLibrary(item, true) }
    } else null

    sealed class ItemAction {
        abstract val action: () -> Unit

        data class Resource(
            val resId: Int, val stringId: Int, override val action: () -> Unit
        ) : ItemAction()

        data class Custom(
            val image: ImageHolder?,
            val placeholder: Int,
            val title: String,
            override val action: () -> Unit
        ) : ItemAction()
    }

    var clicked = false
    inner class ActionAdapter(val list: List<ItemAction>) :
        RecyclerView.Adapter<ActionAdapter.ViewHolder>() {
        inner class ViewHolder(val binding: ItemDialogButtonBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {
                binding.root.setOnClickListener {
                    list[bindingAdapterPosition].action()
                    clicked = true
                    dismiss()
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDialogButtonBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun getItemCount() = list.count()

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val action = list[position]
            val binding = holder.binding
            val colorState = ColorStateList.valueOf(
                binding.root.context.getColor(R.color.button_item)
            )
            when (action) {
                is ItemAction.Resource -> {
                    binding.textView.setText(action.stringId)
                    binding.imageView.setImageResource(action.resId)
                    binding.imageView.imageTintList = colorState
                }

                is ItemAction.Custom -> {
                    binding.textView.text = action.title
                    action.image.loadAsCircle(binding.root) {
                        if (it == null) {
                            binding.imageView.imageTintList = colorState
                            binding.imageView.setImageResource(action.placeholder)
                        } else {
                            binding.imageView.imageTintList = null
                            binding.imageView.setImageDrawable(it)
                        }
                    }
                }
            }
        }
    }

    class LoadingAdapter : RecyclerView.Adapter<LoadingAdapter.ViewHolder>() {
        inner class ViewHolder(val binding: ItemDialogButtonLoadingBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDialogButtonLoadingBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun getItemCount() = 1
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!clicked) return
        parentFragmentManager.setFragmentResult("reload", bundleOf("id" to item.id))
    }
}