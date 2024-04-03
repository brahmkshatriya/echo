package dev.brahmkshatriya.echo.ui.player

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import dev.brahmkshatriya.echo.databinding.ItemPlayerCollapsedBinding
import dev.brahmkshatriya.echo.databinding.ItemPlayerTrackBinding
import dev.brahmkshatriya.echo.player.StreamableTrack
import dev.brahmkshatriya.echo.ui.player.PlayerColors.Companion.defaultPlayerColors
import dev.brahmkshatriya.echo.ui.player.PlayerColors.Companion.getColorsFrom
import dev.brahmkshatriya.echo.ui.settings.LookFragment
import dev.brahmkshatriya.echo.utils.load
import dev.brahmkshatriya.echo.utils.loadWith
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel
import kotlinx.coroutines.flow.Flow

class PlayerTrackAdapter(
    fragment: Fragment
) : LifeCycleListAdapter<StreamableTrack, ItemPlayerTrackBinding>(DiffCallback) {

    private val viewModel by fragment.activityViewModels<PlayerViewModel>()
    private val uiViewModel by fragment.activityViewModels<UiViewModel>()

    object DiffCallback : DiffUtil.ItemCallback<StreamableTrack>() {
        override fun areItemsTheSame(oldItem: StreamableTrack, newItem: StreamableTrack) =
            oldItem.track.id == newItem.track.id

        override fun areContentsTheSame(oldItem: StreamableTrack, newItem: StreamableTrack) =
            oldItem == newItem
    }

    override fun inflateCallback(inflater: LayoutInflater, container: ViewGroup?) =
        ItemPlayerTrackBinding.inflate(inflater, container, false)

    override fun Holder<StreamableTrack, ItemPlayerTrackBinding>.onBind(position: Int) {
        val track = getItem(position)?.track
        observe(uiViewModel.playerSheetOffset) {
            val height = binding.collapsedContainer.root.height
            binding.collapsedContainer.root.run {
                translationY = -height * it
                alpha = 1 - it
            }
            binding.expandedTrackCoverContainer.run {
                translationY = height * (1 - it)
                alpha = it
            }
        }
        observe(uiViewModel.infoSheetOffset) {
            binding.background.alpha = it
        }
        track?.cover.loadWith(binding.expandedTrackCover) {
            binding.collapsedContainer.collapsedTrackCover.load(it)
            val colors = binding.root.context.getPlayerColors(it as? BitmapDrawable)
            binding.root.setBackgroundColor(colors.background)
            binding.collapsedContainer.applyColors(colors)
        }
        binding.collapsedContainer.run {
            collapsedTrackArtist.text = track?.artists?.joinToString(", ") { it.name }
            collapsedTrackTitle.text = track?.title
        }
        fun <T> observeCollapsed(
            flow: Flow<T>,
            block: ItemPlayerCollapsedBinding.(T?) -> Unit
        ) {
            observe(flow) {
                if (viewModel.track.value?.id == track?.id)
                    binding.collapsedContainer.block(it)
                else binding.collapsedContainer.block(null)
            }
        }

        val playPauseListener = viewModel.playPauseListener
        binding.collapsedContainer.collapsedTrackPlayPause
            .addOnCheckedStateChangedListener(playPauseListener)
        observeCollapsed(viewModel.isPlaying) {
            playPauseListener.enabled = false
            collapsedTrackPlayPause.isChecked = it ?: false
            playPauseListener.enabled = true
        }

        binding.collapsedContainer.playerClose.setOnClickListener {
            viewModel.clearQueue()
        }
        observeCollapsed(viewModel.progress) {
            val (current, buffered) = it ?: (0 to 0)
            collapsedBuffer.progress = buffered
            collapsedSeekBar.progress = current
        }
        observeCollapsed(viewModel.buffering) {
            collapsedProgressBar.isVisible = it ?: false
            collapsedTrackPlayPause.isEnabled = it?.not() ?: true
        }
        observeCollapsed(viewModel.totalDuration) {
            collapsedSeekBar.max = it ?: 100
            collapsedBuffer.max = it ?: 100
        }
    }

    private fun Context.getPlayerColors(bitmapDrawable: BitmapDrawable?): PlayerColors {
        val defaultColors = defaultPlayerColors()
        val preferences =
            applicationContext.getSharedPreferences(packageName, Context.MODE_PRIVATE)
        val dynamicPlayer = preferences.getBoolean(LookFragment.DYNAMIC_PLAYER, true)
        val imageColors =
            if (dynamicPlayer) getColorsFrom(bitmapDrawable?.bitmap) else defaultColors
        return imageColors ?: defaultColors
    }

    private fun ItemPlayerCollapsedBinding.applyColors(colors: PlayerColors) {
        root.setBackgroundColor(colors.background)
        collapsedProgressBar.setIndicatorColor(colors.clickable)
        collapsedSeekBar.setIndicatorColor(colors.clickable)
        collapsedBuffer.setIndicatorColor(colors.clickable)
        collapsedBuffer.trackColor = colors.body
        collapsedTrackArtist.setTextColor(colors.body)
        collapsedTrackTitle.setTextColor(colors.body)
    }

}