package dev.brahmkshatriya.echo.ui.media.more

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.paging.LoadState
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.DialogMediaMoreBinding
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.extensions.builtin.offline.OfflineExtension
import dev.brahmkshatriya.echo.extensions.builtin.unified.UnifiedExtension.Companion.EXTENSION_ID
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.common.GridAdapter
import dev.brahmkshatriya.echo.ui.common.GridAdapter.Companion.configureGridLayout
import dev.brahmkshatriya.echo.ui.download.DownloadViewModel
import dev.brahmkshatriya.echo.ui.feed.FeedAdapter
import dev.brahmkshatriya.echo.ui.feed.FeedLoadingAdapter
import dev.brahmkshatriya.echo.ui.feed.FeedLoadingAdapter.Companion.createListener
import dev.brahmkshatriya.echo.ui.feed.viewholders.MediaViewHolder.Companion.icon
import dev.brahmkshatriya.echo.ui.media.MediaFragment
import dev.brahmkshatriya.echo.ui.media.MediaState
import dev.brahmkshatriya.echo.ui.media.MediaDetailsViewModel
import dev.brahmkshatriya.echo.ui.media.more.MoreButton.Companion.button
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.player.audiofx.AudioEffectsBottomSheet
import dev.brahmkshatriya.echo.ui.player.quality.QualitySelectionBottomSheet
import dev.brahmkshatriya.echo.ui.player.sleep.SleepTimerBottomSheet
import dev.brahmkshatriya.echo.ui.playlist.delete.DeletePlaylistBottomSheet
import dev.brahmkshatriya.echo.ui.playlist.edit.EditPlaylistBottomSheet
import dev.brahmkshatriya.echo.ui.playlist.edit.EditPlaylistFragment
import dev.brahmkshatriya.echo.ui.playlist.save.SaveToPlaylistBottomSheet
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoClearedNullable
import kotlinx.coroutines.flow.combine
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class MediaMoreBottomSheet : BottomSheetDialogFragment(R.layout.dialog_media_more) {
    companion object {
        fun newInstance(
            contId: Int,
            extensionId: String,
            item: EchoMediaItem,
            loaded: Boolean,
            fromPlayer: Boolean = false,
            context: EchoMediaItem? = null,
            tabId: String? = null,
            pos: Int? = null,
        ) = MediaMoreBottomSheet().apply {
            arguments = Bundle().apply {
                putInt("contId", contId)
                putString("extensionId", extensionId)
                putSerialized("item", item)
                putBoolean("loaded", loaded)
                putSerialized("context", context)
                putBoolean("fromPlayer", fromPlayer)
                putString("tabId", tabId)
                putInt("pos", pos ?: -1)
            }
        }
    }

    private val args by lazy { requireArguments() }
    private val contId by lazy { args.getInt("contId", -1).takeIf { it != -1 }!! }
    private val extensionId by lazy { args.getString("extensionId")!! }
    private val item by lazy { args.getSerialized<EchoMediaItem>("item")!! }
    private val loaded by lazy { args.getBoolean("loaded") }
    private val itemContext by lazy { args.getSerialized<EchoMediaItem>("context") }
    private val tabId by lazy { args.getString("tabId") }
    private val pos by lazy { args.getInt("pos") }
    private val fromPlayer by lazy { args.getBoolean("fromPlayer") }
    private val delete by lazy { args.getBoolean("delete", false) }

    private val vm by viewModel<MediaDetailsViewModel> {
        parametersOf(false, extensionId, item, loaded, delete)
    }
    private val playerViewModel by activityViewModel<PlayerViewModel>()

    private val actionAdapter by lazy { MoreButtonAdapter() }
    private val headerAdapter by lazy {
        MoreHeaderAdapter({ dismiss() }, {
            openItemFragment(extensionId, item, loaded)
            dismiss()
        })
    }

    private var loadingTextView by autoClearedNullable<TextView>()
    private val loadingAdapter by lazy {
        FeedLoadingAdapter(createListener { vm.refresh() }) {
            val holder = FeedAdapter.LoadingViewHolder(it)
            holder.binding.textView.isVisible = true
            loadingTextView = holder.binding.textView
            holder
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = DialogMediaMoreBinding.bind(view)
        observe(playerViewModel.playerState.current) {
            headerAdapter.onCurrentChanged(it)
        }
        val actionFlow =
            combine(vm.downloadsFlow, vm.itemResultFlow) { _, _ -> }
        observe(actionFlow) {
            val client = vm.extensionFlow.value?.instance?.value()?.getOrNull()
            val result = vm.itemResultFlow.value?.getOrNull()
            val downloads = vm.downloadsFlow.value.filter { it.download.finalFile != null }
            val loaded = if (result != null) true else loaded
            val list = getButtons(client, result, loaded, downloads)
            actionAdapter.submitList(list)
            headerAdapter.item = result?.item ?: item
        }
        observe(vm.itemResultFlow) { result ->
            loadingAdapter.loadState = result?.map { LoadState.NotLoading(false) }?.getOrElse {
                LoadState.Error(it)
            } ?: LoadState.Loading
        }
        configureGridLayout(
            binding.root,
            GridAdapter.Concat(
                headerAdapter,
                actionAdapter,
                loadingAdapter
            ),
            false
        )
    }

    private fun getButtons(
        client: ExtensionClient?,
        state: MediaState?,
        loaded: Boolean,
        downloads: List<Downloader.Info>
    ) = getPlayerButtons() +
            getPlayButtons(client, state?.item ?: item, loaded) +
            getPlaylistEditButtons(client, state, loaded) +
            getDownloadButtons(client, state, downloads) +
            getActionButtons(state) +
            getItemButtons(state?.item ?: item)

    private fun getPlayerButtons() = if (fromPlayer) listOf(
        button("audio_fx", R.string.audio_fx, R.drawable.ic_equalizer) {
            AudioEffectsBottomSheet().show(parentFragmentManager, null)
        },
        button("sleep_timer", R.string.sleep_timer, R.drawable.ic_snooze) {
            SleepTimerBottomSheet().show(parentFragmentManager, null)
        },
        button("quality_selection", R.string.quality_selection, R.drawable.ic_high_quality) {
            QualitySelectionBottomSheet().show(parentFragmentManager, null)
        }
    ) else listOf()

    private fun getPlayButtons(
        client: ExtensionClient?, item: EchoMediaItem, loaded: Boolean
    ) = if (client is TrackClient) listOfNotNull(
        button("play", R.string.play, R.drawable.ic_play) {
            playerViewModel.play(extensionId, item, loaded)
        },
        if (playerViewModel.queue.isNotEmpty())
            button("next", R.string.add_to_next, R.drawable.ic_playlist_play) {
                playerViewModel.addToNext(extensionId, item, loaded)
            }
        else null,
        if (playerViewModel.queue.size > 1)
            button("queue", R.string.add_to_queue, R.drawable.ic_playlist_add) {
                playerViewModel.addToQueue(extensionId, item, loaded)
            }
        else null
    ) else listOf()

    fun getPlaylistEditButtons(
        client: ExtensionClient?, state: MediaState?, loaded: Boolean
    ) = run {
        if (client !is PlaylistEditClient) return@run listOf()
        val item = state?.item ?: item
        val isEditable = item is Playlist && item.isEditable
        listOfNotNull(
            if (loaded) button(
                "save_to_playlist", R.string.save_to_playlist, R.drawable.ic_library_music
            ) {
                SaveToPlaylistBottomSheet.newInstance(extensionId, item)
                    .show(parentFragmentManager, null)
            } else null,
            if (isEditable) button(
                "edit_playlist", R.string.edit_playlist, R.drawable.ic_edit_note
            ) {
                openFragment<EditPlaylistFragment>(
                    EditPlaylistFragment.getBundle(extensionId, item, loaded)
                )
            } else null,
            if (isEditable) button(
                "delete_playlist", R.string.delete_playlist, R.drawable.ic_delete
            ) {
                DeletePlaylistBottomSheet.show(requireParentFragment(), extensionId, item, loaded)
            } else null,
            if (itemContext is Playlist && item is Track) button(
                "remove_from_playlist", R.string.remove, R.drawable.ic_delete
            ) {
                EditPlaylistBottomSheet.newInstance(
                    extensionId, itemContext as Playlist, tabId, pos
                ).show(parentFragmentManager, null)
            } else null
        )
    }

    fun getDownloadButtons(
        client: ExtensionClient?, state: MediaState?, downloads: List<Downloader.Info>
    ) = run {
        val item = state?.item ?: item
        val shouldShowDelete = when (item) {
            is Track -> downloads.any { it.download.trackId == item.id }
            else -> downloads.any { it.context?.itemId == item.id }
        }
        val downloadable =
            state != null && client is TrackClient && state.item.extras[EXTENSION_ID] != OfflineExtension.metadata.id

        listOfNotNull(
            if (downloadable) button(
                "download", R.string.download, R.drawable.ic_download_for_offline
            ) {
                val downloadViewModel by activityViewModel<DownloadViewModel>()
                downloadViewModel.addToDownload(requireActivity(), extensionId, item)
            } else null,
            if (shouldShowDelete) button(
                "delete_download", R.string.delete_download, R.drawable.ic_delete
            ) {
                val downloadViewModel by activityViewModel<DownloadViewModel>()
                downloadViewModel.deleteDownload(item)
            } else null
        )

    }

    fun getActionButtons(
        state: MediaState?,
    ) = listOfNotNull(
        if (state?.isFollowed != null) button(
            "follow", if (state.isFollowed) R.string.unfollow else R.string.follow,
            if (state.isFollowed) R.drawable.ic_heart_filled_40dp else R.drawable.ic_heart_outline_40dp
        ) {
            vm.followItem(!state.isFollowed)
        } else null,
        if (state?.isSaved != null) button(
            "save_to_library",
            if (state.isSaved) R.string.remove_from_library else R.string.save_to_library,
            if (state.isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
        ) {
            vm.saveToLibrary(!state.isSaved)
        } else null,
        if (state?.isLiked != null) button(
            "like", if (state.isLiked) R.string.unlike else R.string.like,
            if (state.isLiked) R.drawable.ic_heart_filled_40dp else R.drawable.ic_heart_outline_40dp
        ) {
            vm.likeTrack(!state.isLiked)
        } else null,
        if (state?.showRadio == true) button(
            "radio", R.string.radio, R.drawable.ic_sensors
        ) {
            playerViewModel.radio(extensionId, state.item, true)
        } else null,
        if (state?.showShare == true) button(
            "share", R.string.share, R.drawable.ic_share
        ) {
            vm.onShare()
        } else null
    )

    private fun getItemButtons(item: EchoMediaItem) = when (item) {
        is Track -> item.artists + listOfNotNull(item.album)
        is EchoMediaItem.Lists -> item.artists
        is Artist -> listOf()
    }.map {
        button(it.id, it.title, it.icon) {
            openItemFragment(extensionId, it)
        }
    }

    private inline fun <reified T : Fragment> openFragment(bundle: Bundle) {
        parentFragmentManager.findFragmentById(contId)!!
            .openFragment<T>(null, bundle)
    }

    private fun openItemFragment(
        extensionId: String?, item: EchoMediaItem?, loaded: Boolean = false
    ) {
        extensionId ?: return
        item ?: return
        openFragment<MediaFragment>(MediaFragment.getBundle(extensionId, item, loaded))
        dismiss()
    }
}