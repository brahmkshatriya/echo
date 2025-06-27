package dev.brahmkshatriya.echo.ui.player

import android.graphics.Bitmap
import android.graphics.Outline
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.databinding.ItemClickPanelsBinding
import dev.brahmkshatriya.echo.databinding.ItemPlayerTrackBinding
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.MediaItemUtils.unloadedCover
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.common.UiViewModel
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyHorizontalInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.player.PlayerColors.Companion.defaultPlayerColors
import dev.brahmkshatriya.echo.utils.image.ImageUtils.getCachedDrawable
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadWithThumb
import dev.brahmkshatriya.echo.utils.ui.GestureListener
import dev.brahmkshatriya.echo.utils.ui.GestureListener.Companion.handleGestures
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import dev.brahmkshatriya.echo.utils.ui.UiUtils.isLandscape
import dev.brahmkshatriya.echo.utils.ui.UiUtils.isRTL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.max

class PlayerTrackAdapter(
    private val uiViewModel: UiViewModel,
    private val current: MutableStateFlow<PlayerState.Current?>,
    private val listener: Listener
) : ListAdapter<MediaItem, PlayerTrackAdapter.ViewHolder>(DiffCallback) {

    interface Listener {
        fun onClick()
        fun onLongClick() {}
        fun onStartDoubleClick() {}
        fun onEndDoubleClick() {}
    }

    object DiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
            oldItem.mediaId == newItem.mediaId

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(
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
            val targetPosX = collapsedPadding + if (context.isRTL()) insets.end else insets.start
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
            if (isLandscape) binding.clickPanel.root.scaleX = 0.5f + 0.5f * inv
            val extraY = if (!isPlayerVisible) 0f else {
                val toMoveY = binding.playerControlsPlaceholder.top - cover.top
                toMoveY * inv
            }
            val extraX = if (!isPlayerVisible) 0f else {
                val toMoveX = binding.playerControlsPlaceholder.left - cover.left
                toMoveX * inv
            }
            cover.run {
                scaleX = if (!isPlayerVisible) 1 + (targetScale - 1) * offset else targetScale
                scaleY = scaleX
                translationX = targetX * offset + extraX
                translationY = targetY * offset + extraY
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

        fun updateColors() {
            binding.playerCollapsed.run {
                val colors = uiViewModel.playerColors.value ?: context.defaultPlayerColors()
                collapsedTrackTitle.setTextColor(colors.onBackground)
                collapsedTrackArtist.setTextColor(colors.onBackground)
            }
        }

        private var bitmap: Result<Bitmap?> = Result.failure(Exception())
        fun applyBitmap() {
            if (bitmap.isFailure) return
            val index = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return
            val item = getItem(index) ?: return
            val curr = current.value?.mediaItem
            if (curr != item) return
            val bitmap = bitmap.getOrNull()
            currentBitmapListener?.invoke(bitmap)
        }

        fun bind(item: MediaItem?) {
            binding.playerCollapsed.run {
                collapsedTrackTitle.text = item?.track?.title
                collapsedTrackArtist.text = item?.track?.toMediaItem()?.subtitleWithE
            }
            val old = item?.unloadedCover?.getCachedDrawable(binding.root.context)
            item?.track?.cover.loadWithThumb(binding.playerTrackCover, old) {
                val image = it
                    ?: ResourcesCompat.getDrawable(resources, R.drawable.art_music, context.theme)
                setImageDrawable(image)
                bitmap = Result.success(it?.toBitmap())
                applyBitmap()
            }
            updateInsets()
            updateColors()
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
            binding.clickPanel.configureClicking(listener, uiViewModel)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPlayerTrackBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
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
        holder.updateColors()
        holder.applyBitmap()
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.updateInsets()
        holder.updateColors()
        holder.applyBitmap()
    }

    private fun onEachViewHolder(block: ViewHolder.() -> Unit) {
        val recyclerView = recyclerView ?: return
        recyclerView.run {
            for (it in 0 until childCount) {
                val viewHolder = getChildViewHolder(getChildAt(it)) as? ViewHolder
                viewHolder?.block()
            }
        }
    }

    fun moreOffsetUpdated() = onEachViewHolder { updateCollapsed() }
    fun playerOffsetUpdated() = onEachViewHolder { updateCollapsed() }
    fun playerSheetStateUpdated() = onEachViewHolder { updateInsets() }
    fun insetsUpdated() = onEachViewHolder { updateInsets() }
    fun playerControlsHeightUpdated() = onEachViewHolder { updateInsets() }
    fun onColorsUpdated() = onEachViewHolder { updateColors() }
    fun onCurrentUpdated() {
        onEachViewHolder { applyBitmap() }
        if (current.value == null) currentBitmapListener?.invoke(null)
    }

    private var isPlayerVisible = false
    fun updatePlayerVisibility(visible: Boolean) {
        isPlayerVisible = visible
        onEachViewHolder { updateInsets() }
    }

    var currentBitmapListener: ((Bitmap?) -> Unit)? = null

    companion object {
        fun ItemClickPanelsBinding.configureClicking(listener: Listener, uiViewModel: UiViewModel) {
            start.handleGestures(object : GestureListener {
                override val onClick = listener::onClick
                override val onLongClick = listener::onLongClick
                override val onDoubleClick: (() -> Unit)?
                    get() = if (uiViewModel.playerSheetState.value != STATE_EXPANDED) null
                    else listener::onStartDoubleClick
            })
            end.handleGestures(object : GestureListener {
                override val onClick = listener::onClick
                override val onLongClick = listener::onLongClick
                override val onDoubleClick: (() -> Unit)?
                    get() = if (uiViewModel.playerSheetState.value != STATE_EXPANDED) null
                    else listener::onEndDoubleClick
            })
        }
    }
}