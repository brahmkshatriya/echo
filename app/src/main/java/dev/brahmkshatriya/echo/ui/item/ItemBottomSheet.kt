package dev.brahmkshatriya.echo.ui.item

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.ArtistFollowClient
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.databinding.DialogMediaItemBinding
import dev.brahmkshatriya.echo.databinding.ItemDialogButtonBinding
import dev.brahmkshatriya.echo.databinding.ItemDialogButtonLoadingBinding
import dev.brahmkshatriya.echo.offline.OfflineExtension
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.ui.adapter.MediaContainerViewHolder.Media.Companion.bind
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.editplaylist.AddToPlaylistBottomSheet
import dev.brahmkshatriya.echo.ui.exception.ExceptionFragment.Companion.copyToClipboard
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.getParcel
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.DownloadViewModel
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import dev.brahmkshatriya.echo.viewmodels.UiViewModel

@AndroidEntryPoint
class ItemBottomSheet : BottomSheetDialogFragment() {
    companion object {
        fun newInstance(
            clientId: String, item: EchoMediaItem, loaded: Boolean, fromPlayer: Boolean
        ) = ItemBottomSheet().apply {
            arguments = Bundle().apply {
                putString("clientId", clientId)
                putParcelable("item", item)
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
    private val item by lazy { args.getParcel<EchoMediaItem>("item")!! }
    private val fromPlayer by lazy { args.getBoolean("fromPlayer") }
    private val loaded by lazy { args.getBoolean("loaded") }
    private val extension by lazy { playerViewModel.extensionListFlow.getExtension(clientId) }
    private val client by lazy { extension?.client }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogMediaItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.itemContainer.run {
            more.run {
                setOnClickListener { dismiss() }
                setIconResource(R.drawable.ic_close)
                contentDescription = context.getString(R.string.close)
            }
            bind(item)
            if (!loaded) root.setOnClickListener {
                openItemFragment(item)
                dismiss()
            }
        }
        if (!loaded) {
            binding.recyclerView.adapter =
                ConcatAdapter(ActionAdapter(getActions(item, false)), LoadingAdapter())

            viewModel.item = item
            viewModel.extension = extension
            viewModel.initialize()
            observe(viewModel.itemFlow) {
                if (it != null) {
                    binding.itemContainer.bind(it)
                    binding.recyclerView.adapter = ActionAdapter(getActions(it, true))
                }
            }
        } else {
            binding.recyclerView.adapter = ActionAdapter(getActions(item, true))
        }
        observe(viewModel.shareLink) {
            requireContext().copyToClipboard(it, it)
        }
    }

    private fun openItemFragment(item: EchoMediaItem) {
        requireActivity().openFragment(ItemFragment.newInstance(clientId, item))
        uiViewModel.collapsePlayer()
    }

    private fun getActions(item: EchoMediaItem, loaded: Boolean): List<ItemAction> = when (item) {
        is EchoMediaItem.Lists -> {
            (if (client is TrackClient)
                listOfNotNull(
                    ItemAction.Resource(R.drawable.ic_play, R.string.play) {
                        playerViewModel.play(clientId, item, 0)
                    },
                    ItemAction.Resource(R.drawable.ic_playlist_play, R.string.play_next) {
                        playerViewModel.addToQueue(clientId, item, false)
                    },
                    ItemAction.Resource(R.drawable.ic_playlist_add, R.string.add_to_queue) {
                        playerViewModel.addToQueue(clientId, item, true)
                    },
                    if (client !is OfflineExtension)
                        ItemAction.Resource(R.drawable.ic_download_for_offline, R.string.download) {
                            downloadViewModel.addToDownload(requireActivity(), clientId, item)
                        }
                    else null
                ) else listOf()
                    ) + when (item) {
                is EchoMediaItem.Lists.AlbumItem -> {
                    listOfNotNull(
                        if (client is LibraryClient)
                            ItemAction.Resource(
                                R.drawable.ic_bookmark_outline, R.string.save_to_playlist
                            ) {
                                AddToPlaylistBottomSheet.newInstance(clientId, item)
                                    .show(parentFragmentManager, null)
                            }
                        else null,
                        if (client is RadioClient)
                            ItemAction.Resource(R.drawable.ic_radio, R.string.radio) {
                                playerViewModel.radio(clientId, item)
                            }
                        else null,
                    ) + item.album.artists.map {
                        ItemAction.Custom(it.cover, R.drawable.ic_artist, it.name) {
                            openItemFragment(it.toMediaItem())
                        }
                    }
                }

                is EchoMediaItem.Lists.PlaylistItem -> {
                    listOfNotNull(
                        if (client is RadioClient)
                            ItemAction.Resource(R.drawable.ic_radio, R.string.radio) {
                                playerViewModel.radio(clientId, item)
                            }
                        else null,
                        if (client is LibraryClient)
                            ItemAction.Resource(
                                R.drawable.ic_bookmark_outline, R.string.save_to_playlist
                            ) {
                                AddToPlaylistBottomSheet.newInstance(clientId, item)
                                    .show(parentFragmentManager, null)
                            }
                        else null,
                        if (client is LibraryClient && item.playlist.isEditable)
                            ItemAction.Resource(R.drawable.ic_delete, R.string.delete_playlist) {
                                viewModel.deletePlaylist(clientId, item.playlist)
                            }
                        else null,
                    ) + item.playlist.authors.map {
                        ItemAction.Custom(it.cover, R.drawable.ic_artist, it.name) {
                            openItemFragment(it.toMediaItem())
                        }
                    }
                }
            }
        }

        is EchoMediaItem.Profile -> {
            if (item is EchoMediaItem.Profile.ArtistItem) listOfNotNull(
                if (client is RadioClient)
                    ItemAction.Resource(R.drawable.ic_radio, R.string.radio) {
                        playerViewModel.radio(clientId, item)
                    }
                else null,
                if (client is ArtistFollowClient)
                    if (!item.artist.isFollowing)
                        ItemAction.Resource(R.drawable.ic_heart_outline, R.string.follow) {
                            createSnack("Not implemented")
                        }
                    else ItemAction.Resource(R.drawable.ic_heart_filled, R.string.unfollow) {
                        createSnack("Not implemented")
                    }
                else null
            )
            else listOf()
        }

        is EchoMediaItem.TrackItem -> {
            (if (client is TrackClient && !fromPlayer)
                listOf(
                    ItemAction.Resource(R.drawable.ic_play, R.string.play) {
                        playerViewModel.play(clientId, item.track)
                    },
                    ItemAction.Resource(R.drawable.ic_playlist_play, R.string.play_next) {
                        playerViewModel.addToQueue(clientId, item.track, false)
                    },
                    ItemAction.Resource(R.drawable.ic_playlist_add, R.string.add_to_queue) {
                        playerViewModel.addToQueue(clientId, item.track, true)
                    }
                )
            else listOf()) + listOfNotNull(
                if (fromPlayer)
                    ItemAction.Resource(R.drawable.ic_equalizer, R.string.equalizer) {
                        createSnack("Not implemented")
                    }
                else null,
                if (fromPlayer)
                    ItemAction.Resource(R.drawable.ic_snooze, R.string.sleep_timer) {
                        createSnack("Not implemented")
                    }
                else null,
                if (client !is OfflineExtension && client is TrackClient)
                    ItemAction.Resource(R.drawable.ic_download_for_offline, R.string.download) {
                        downloadViewModel.addToDownload(requireActivity(), clientId, item)
                    }
                else null,
                if (client is LibraryClient)
                    ItemAction.Resource(R.drawable.ic_bookmark_outline, R.string.save_to_playlist) {
                        AddToPlaylistBottomSheet.newInstance(clientId, item)
                            .show(parentFragmentManager, null)
                    }
                else null,
                if (client is RadioClient)
                    ItemAction.Resource(R.drawable.ic_radio, R.string.radio) {
                        playerViewModel.radio(clientId, item)
                    }
                else null,
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
        }
    } + listOfNotNull(
        if (client is ShareClient && loaded) ItemAction.Resource(
            R.drawable.ic_forward,
            R.string.share
        ) {
            val shareClient = client as ShareClient
            viewModel.onShare(shareClient, item)
        } else null
    )

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

    inner class ActionAdapter(val list: List<ItemAction>) :
        RecyclerView.Adapter<ActionAdapter.ViewHolder>() {
        inner class ViewHolder(val binding: ItemDialogButtonBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {
                binding.root.setOnClickListener {
                    list[bindingAdapterPosition].action()
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
                    binding.imageView.imageTintList = colorState
                    action.image.loadInto(binding.imageView, action.placeholder)
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
}