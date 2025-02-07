package dev.brahmkshatriya.echo.ui.player

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.core.view.doOnDetach
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.databinding.ItemPlayerTrackBinding
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.extensions.isClient
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.item.ItemBottomSheet
import dev.brahmkshatriya.echo.ui.item.ItemFragment
import dev.brahmkshatriya.echo.ui.player.viewholder.DefaultViewHolder
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import dev.brahmkshatriya.echo.viewmodels.UiViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.max

@OptIn(UnstableApi::class)
class PlayerTrackAdapter(
    private val listener: Listener,
    private val cache: SimpleCache,
    private val settings: SharedPreferences,
    private val isTrackLikeClient: (String, (Boolean) -> Unit) -> Unit
) : ListAdapter<MediaItem, PlayerTrackAdapter.ViewHolder>(DiffCallback) {

    interface Listener {
        fun onPlayPause(play: Boolean)
        fun onRepeat(mode: Int)
        fun onShuffle(shuffle: Boolean)
        fun onSeekTo(position: Long)
        fun onNext()
        fun onPrevious()
        fun onItemLiked(mediaItem: MediaItem, liked: Boolean)
        fun onItemClicked(item: Pair<String?, EchoMediaItem>)
        fun onMoreClicked(mediaItem: MediaItem)
        fun onChangePlayerState(state: Int)
        fun onShowBackground(show: Boolean)
    }

    object DiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
            oldItem.mediaId == newItem.mediaId

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem) =
            oldItem == newItem

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return DefaultViewHolder(
            ItemPlayerTrackBinding.inflate(inflater, parent, false),
            listener,
            cache,
            settings,
            isTrackLikeClient,
            players
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
        updateViewHolder(holder)
    }

    private fun updateViewHolder(holder: ViewHolder) {
        holder.onPlayer(player)
        holder.onCurrentChanged(current)
        holder.onRepeatModeChanged(repeatMode)
        holder.onShuffleChanged(shuffle)
        holder.onNextEnabled(nextEnabled)
        holder.onPreviousEnabled(previousEnabled)
        holder.onPlayPause(isPlaying)
        holder.onBufferingChanged(buffering)
        holder.onProgressChanged(progress.first, progress.second)
        holder.onTotalDurationChanged(totalDuration)

        holder.onPlayerOffsetChanged(playerOffset)
        holder.onPlayerStateChanged(playerState)
        holder.onPlayerBgVisibleChanged(playerBgVisible)
        holder.onInfoSheetOffsetChanged(infoSheetOffset)
        holder.onInfoSheetStateChanged(infoSheetState)
        holder.onSystemInsetsChanged(systemInsets)
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        updateViewHolder(holder)
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.cleanUp()
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.cleanUp()
    }

    private val players = mutableListOf<Player>()
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        recyclerView.doOnDetach {
            players.forEach { it.release() }
            players.clear()
        }
    }

    private lateinit var recyclerView: RecyclerView
    private fun onEachViewHolder(block: ViewHolder.() -> Unit) {
        for (i in 0 until recyclerView.childCount) {
            val holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i)) as ViewHolder
            holder.block()
        }
    }

    abstract class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: MediaItem)
        open fun cleanUp() {}

        // Player Changes
        open fun onPlayer(player: Player?) {}
        open fun onCurrentChanged(current: Current?) {}
        open fun onRepeatModeChanged(mode: Int) {}
        open fun onShuffleChanged(shuffled: Boolean) {}
        open fun onNextEnabled(enabled: Boolean) {}
        open fun onPreviousEnabled(enabled: Boolean) {}
        open fun onPlayPause(play: Boolean) {}
        open fun onBufferingChanged(buffering: Boolean) {}
        open fun onProgressChanged(current: Int, buffered: Int) {}
        open fun onTotalDurationChanged(totalDuration: Int?) {}

        // UI Changes
        open fun onPlayerOffsetChanged(it: Float) {}
        open fun onPlayerStateChanged(state: Int) {}
        open fun onPlayerBgVisibleChanged(visible: Boolean) {}
        open fun onInfoSheetOffsetChanged(offset: Float) {}
        open fun onInfoSheetStateChanged(state: Int) {}
        open fun onSystemInsetsChanged(insets: UiViewModel.Insets) {}
        open fun onCombinedInsetsChanged(insets: UiViewModel.Insets) {}
    }

    companion object {
        fun newInstance(
            fragment: Fragment,
            recyclerView: RecyclerView
        ): PlayerTrackAdapter {
            val playerViewModel by fragment.activityViewModels<PlayerViewModel>()
            val uiViewModel by fragment.activityViewModels<UiViewModel>()

            val listener = object : Listener {
                override fun onPlayPause(play: Boolean) {
                    playerViewModel.withBrowser {
                        if (play) {
                            it.prepare()
                            it.play()
                        } else it.pause()
                    }
                }

                override fun onRepeat(mode: Int) {
                    playerViewModel.withBrowser { it.repeatMode = mode }
                }

                override fun onShuffle(shuffle: Boolean) {
                    playerViewModel.withBrowser { it.shuffleModeEnabled = shuffle }
                }

                override fun onSeekTo(position: Long) {
                    playerViewModel.withBrowser { it.seekTo(max(0, position)) }
                }

                override fun onNext() {
                    playerViewModel.withBrowser {
                        it.prepare()
                        it.seekToNext()
                    }
                }

                override fun onPrevious() {
                    playerViewModel.withBrowser {
                        it.prepare()
                        it.seekToPrevious()
                    }
                }

                override fun onItemLiked(mediaItem: MediaItem, liked: Boolean) {
                    playerViewModel.likeTrack(liked)
                }

                override fun onMoreClicked(mediaItem: MediaItem) {
                    val extensionId = mediaItem.extensionId
                    val item = mediaItem.track.toMediaItem()
                    val loaded = mediaItem.isLoaded
                    ItemBottomSheet.newInstance(extensionId, item, loaded, true)
                        .show(fragment.parentFragmentManager, null)
                }

                override fun onItemClicked(item: Pair<String?, EchoMediaItem>) {
                    val (clientId, media) = item
                    if (clientId == null) {
                        fragment.createSnack(fragment.requireContext().noClient())
                        return
                    }
                    fragment.requireActivity()
                        .openFragment(ItemFragment.newInstance(clientId, media))
                    uiViewModel.collapsePlayer()
                }

                override fun onChangePlayerState(state: Int) {
                    uiViewModel.changePlayerState(state)
                }

                override fun onShowBackground(show: Boolean) {
                    uiViewModel.changeBgVisible(show)
                }
            }

            fun isTrackLikeClient(extensionId: String, callback: (Boolean) -> Unit) =
                fragment.lifecycleScope.launch {
                    val isClient = playerViewModel.extensionListFlow.getExtension(extensionId)
                        ?.isClient<TrackLikeClient>() ?: false
                    callback(isClient)
                }

            val adapter = PlayerTrackAdapter(
                listener,
                playerViewModel.cache,
                playerViewModel.settings,
                ::isTrackLikeClient
            )

            adapter.recyclerView = recyclerView
            fun <T> observe(flow: Flow<T>, block: PlayerTrackAdapter.(T) -> Unit) =
                fragment.observe(flow) {
                    adapter.block(it)
                }

            playerViewModel.run {
                observe(browser) { onPlayer(it) }
                observe(currentFlow) { onCurrentUpdated(it) }
                observe(repeatMode) { onRepeatModeChanged(it) }
                observe(shuffleMode) { onShuffleChanged(it) }
                observe(nextEnabled) { onNextEnabled(it) }
                observe(previousEnabled) { onPreviousEnabled(it) }
                observe(isPlaying) { onPlayPause(it) }
                observe(buffering) { onBufferingChanged(it) }
                observe(progress) { onProgressChanged(it.first, it.second) }
                observe(totalDuration) { onTotalDurationChanged(it) }
            }

            uiViewModel.run {
                observe(playerSheetOffset) { onPlayerOffsetChanged(it) }
                observe(playerSheetState) { onPlayerStateChanged(it) }
                observe(playerBgVisibleState) { onPlayerBgVisibleChanged(it) }
                observe(infoSheetOffset) { onInfoSheetOffsetChanged(it) }
                observe(infoSheetState) { onInfoSheetStateChanged(it) }
                observe(systemInsets) { onSystemInsetsChanged(it) }
                observe(combined) { onCombinedInsetsChanged(it) }
            }

            return adapter
        }
    }

    private var player: Player? = null
    private fun onPlayer(player: Player?) {
        this.player = player
        onEachViewHolder { onPlayer(player) }
    }

    private var current: Current? = null
    private fun onCurrentUpdated(current: Current?) {
        this.current = current
        onEachViewHolder { onCurrentChanged(current) }
    }

    private var shuffle = false
    private fun onShuffleChanged(shuffle: Boolean) {
        this.shuffle = shuffle
        onEachViewHolder { onShuffleChanged(shuffle) }
    }

    private var repeatMode = 0
    private fun onRepeatModeChanged(mode: Int) {
        repeatMode = mode
        onEachViewHolder { onRepeatModeChanged(mode) }
    }

    private var nextEnabled = false
    private fun onNextEnabled(enabled: Boolean) {
        nextEnabled = enabled
        onEachViewHolder { onNextEnabled(enabled) }
    }

    private var previousEnabled = false
    private fun onPreviousEnabled(enabled: Boolean) {
        previousEnabled = enabled
        onEachViewHolder { onPreviousEnabled(enabled) }
    }

    private var isPlaying = false
    private fun onPlayPause(play: Boolean) {
        isPlaying = play
        onEachViewHolder { onPlayPause(play) }
    }

    private var buffering = false
    private fun onBufferingChanged(buffering: Boolean) {
        this.buffering = buffering
        onEachViewHolder { onBufferingChanged(buffering) }
    }

    private var progress = 0 to 0
    private fun onProgressChanged(current: Int, buffered: Int) {
        progress = current to buffered
        onEachViewHolder { onProgressChanged(current, buffered) }
    }

    private var totalDuration: Int? = null
    private fun onTotalDurationChanged(totalDuration: Int?) {
        this.totalDuration = totalDuration
        onEachViewHolder { onTotalDurationChanged(totalDuration) }
    }

    private var playerOffset = 0f
    private fun onPlayerOffsetChanged(it: Float) {
        playerOffset = it
        onEachViewHolder { onPlayerOffsetChanged(it) }
    }

    private var playerState = 0
    private fun onPlayerStateChanged(state: Int) {
        playerState = state
        onEachViewHolder { onPlayerStateChanged(state) }
    }

    private var playerBgVisible = false
    private fun onPlayerBgVisibleChanged(visible: Boolean) {
        playerBgVisible = visible
        onEachViewHolder { onPlayerBgVisibleChanged(visible) }
    }

    private var infoSheetOffset = 0f
    private fun onInfoSheetOffsetChanged(offset: Float) {
        infoSheetOffset = offset
        onEachViewHolder { onInfoSheetOffsetChanged(offset) }
    }

    private var infoSheetState = 0
    private fun onInfoSheetStateChanged(state: Int) {
        infoSheetState = state
        onEachViewHolder { onInfoSheetStateChanged(state) }
    }

    private var systemInsets = UiViewModel.Insets(0, 0, 0, 0)
    private fun onSystemInsetsChanged(insets: UiViewModel.Insets) {
        systemInsets = insets
        onEachViewHolder { onSystemInsetsChanged(insets) }
    }

    private var combinedInsets = UiViewModel.Insets(0, 0, 0, 0)
    private fun onCombinedInsetsChanged(insets: UiViewModel.Insets) {
        combinedInsets = insets
        onEachViewHolder { onCombinedInsetsChanged(insets) }
    }
}