package dev.brahmkshatriya.echo.ui.player

import android.graphics.Outline
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.databinding.ItemPlayerTrackBinding
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.MediaItemUtils.unloadedCover
import dev.brahmkshatriya.echo.ui.UiViewModel
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyHorizontalInsets
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.utils.image.ImageUtils.getCachedDrawable
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadWithThumb
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import dev.brahmkshatriya.echo.utils.ui.UiUtils.isLandscape
import dev.brahmkshatriya.echo.utils.ui.UiUtils.isRTL
import kotlin.math.max

class PlayerTrackAdapter(
    private val uiViewModel: UiViewModel
) : ListAdapter<MediaItem, PlayerTrackAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
            oldItem.mediaId == newItem.mediaId

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem == newItem
        }
    }

    class ViewHolder(
        private val uiViewModel: UiViewModel,
        private val binding: ItemPlayerTrackBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val context = binding.root.context

        private val collapsedPadding = 8.dpToPx(context)
        private val targetZ = collapsedPadding.toFloat()
        private val size = binding.root.resources.getDimension(R.dimen.collapsed_cover_size).toInt()
        private var targetScale = 0f
        private var targetX = 0
        private var targetY = 0

        private val cover = binding.playerTrackCoverContainer
        private var currentCoverHeight = size
        private var currCoverRound = 0f
        private val isLandscape = context.isLandscape()
        fun updateCollapsed() = uiViewModel.run {
            val insets = if (!isLandscape) systemInsets.value else getCombined()
            val targetPosX =

                collapsedPadding + if (context.isRTL()) insets.end else insets.start
            val targetPosY = if (playerSheetState.value != STATE_EXPANDED) 0
            else collapsedPadding + systemInsets.value.top
            targetX = targetPosX - cover.left
            targetY = targetPosY - cover.top
            currentCoverHeight = cover.height.takeIf { it > 0 } ?: currentCoverHeight
            targetScale = size.toFloat() / currentCoverHeight

            val (collapsedY, offset) = if (playerSheetState.value == STATE_EXPANDED)
                systemInsets.value.top to if (isLandscape) 0f else moreSheetOffset.value
            else -collapsedPadding to 1 - max(0f, playerSheetOffset.value)

            val inv = 1 - offset
            binding.playerCollapsed.root.run {
                translationY = collapsedY - size * inv * 2
                alpha = offset
            }
            cover.run {
                scaleX = 1 + (targetScale - 1) * offset
                scaleY = scaleX
                translationX = targetX * offset
                translationY = targetY * offset
                translationZ = targetZ * (1 - offset)
                currCoverRound = collapsedPadding / scaleX
                invalidateOutline()
            }
        }

        fun updateInsets() = uiViewModel.run {
            val (v, h) = if (!isLandscape) 64 to 0 else 0 to 24
            binding.constraintLayout.applyInsets(systemInsets.value, v, h)
            val insets = if (isLandscape) getCombined() else systemInsets.value
            binding.playerCollapsed.root.applyHorizontalInsets(insets)
            binding.playerControlsPlaceholder.run {
                updateLayoutParams {
                    height = playerControlsHeight.value
                }
                doOnLayout {
                    updateCollapsed()
                    cover.doOnLayout { updateCollapsed() }
                }
            }

            updateCollapsed()
        }

        fun bind(item: MediaItem?) {
            binding.playerCollapsed.run {
                collapsedTrackTitle.text = item?.track?.title
                collapsedTrackArtist.text = item?.track?.toMediaItem()?.subtitleWithE
            }
            val old = item?.unloadedCover?.getCachedDrawable(binding.root.context)
            item?.track?.cover.loadWithThumb(binding.playerTrackCover, old, R.drawable.art_problem)
            updateInsets()
        }

        init {
            cover.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(
                        0, 0, currentCoverHeight, currentCoverHeight, currCoverRound
                    )
                }
            }
            cover.clipToOutline = true
            cover.doOnLayout { updateInsets() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPlayerTrackBinding.inflate(inflater, parent, false)
        return ViewHolder(uiViewModel, binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    var recyclerView: RecyclerView? = null
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.updateInsets()
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.updateInsets()
    }

    private fun onEachViewHolder(block: ViewHolder.() -> Unit) {
        val recyclerView = recyclerView ?: return
        recyclerView.run {
            (0 until childCount).forEach {
                val viewHolder = getChildViewHolder(getChildAt(it)) as? ViewHolder
                    ?: return@forEach
                viewHolder.block()
            }
        }
    }

    fun moreOffsetUpdated() = onEachViewHolder { updateCollapsed() }
    fun playerOffsetUpdated() = onEachViewHolder { updateCollapsed() }
    fun playerSheetStateUpdated() = onEachViewHolder { updateInsets() }
    fun systemInsetsUpdated() = onEachViewHolder { updateInsets() }
    fun playerControlsHeightUpdated() = onEachViewHolder { updateInsets() }
}