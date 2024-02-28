package dev.brahmkshatriya.echo.player.ui

import android.animation.ObjectAnimator
import android.graphics.drawable.Animatable
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.START
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_DRAGGING
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_SETTLING
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.checkbox.MaterialCheckBox.OnCheckedStateChangedListener
import com.google.android.material.checkbox.MaterialCheckBox.STATE_CHECKED
import com.google.android.material.slider.Slider
import com.google.android.material.slider.Slider.OnSliderTouchListener
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.player.Global
import dev.brahmkshatriya.echo.player.PlayerHelper.Companion.toTimeString
import dev.brahmkshatriya.echo.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.adapters.PlaylistAdapter
import dev.brahmkshatriya.echo.utils.emit
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.observe
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.math.max
import kotlin.math.min


fun createPlayerUI(
    activity: MainActivity
) {

    val playerBinding = activity.binding.bottomPlayer
    val playlistBinding = playerBinding.bottomPlaylist

    val container = activity.binding.bottomPlayerContainer as View
    val playlistContainer = playerBinding.bottomPlaylistContainer as View

    val playerViewModel: PlayerViewModel by activity.viewModels()
    val uiViewModel: PlayerUIViewModel by activity.viewModels()


    // Apply the UI Changes

    val navView = activity.binding.navView
    val bottomPlayerBehavior = BottomSheetBehavior.from(container)
    val bottomPlaylistBehavior = BottomSheetBehavior.from(playlistContainer)

    container.setOnClickListener {
        bottomPlayerBehavior.state = STATE_EXPANDED
    }

    bottomPlaylistBehavior.addBottomSheetCallback(object :
        BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            bottomPlayerBehavior.isDraggable = newState == STATE_COLLAPSED

            if (newState == STATE_SETTLING || newState == STATE_DRAGGING) return
            playerBinding.expandedSeekBar.isEnabled = newState != STATE_EXPANDED
            PlayerBackButtonHelper.playlistState.value = newState
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            val offset = 1 - slideOffset
            playlistBinding.root.translationY = -uiViewModel.playlistTranslationY * offset
            playlistBinding.playlistRecyclerContainer.alpha = slideOffset
            playerBinding.expandedBackground.alpha = slideOffset
        }
    })
    playlistBinding.playlistTitleIcon.setOnClickListener {
        bottomPlaylistBehavior.apply {
            state = if (state == STATE_EXPANDED) STATE_COLLAPSED else STATE_EXPANDED
        }
    }

    val collapsedCoverSize = activity.resources.getDimension(R.dimen.collapsed_cover_size).toInt()
    bottomPlayerBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            bottomPlayerBehavior.isHideable = newState != STATE_EXPANDED

            if (newState == STATE_SETTLING || newState == STATE_DRAGGING) return
            PlayerBackButtonHelper.playerSheetState.value = newState
            if (newState == STATE_HIDDEN)
                Global.clearQueue(activity.lifecycleScope)
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            val offset = max(0f, slideOffset)
            playerBinding.collapsedContainer.translationY = -collapsedCoverSize * offset
            playerBinding.expandedContainer.translationY = collapsedCoverSize * (1 - offset)
            navView.translationY = uiViewModel.bottomNavTranslateY * offset
        }
    })

    PlayerBackButtonHelper.bottomSheetBehavior = bottomPlayerBehavior
    PlayerBackButtonHelper.playlistBehavior = bottomPlaylistBehavior

    container.post {
        bottomPlayerBehavior.state = PlayerBackButtonHelper.playerSheetState.value
        bottomPlaylistBehavior.state = PlayerBackButtonHelper.playlistState.value
        container.translationY = 0f
    }
    activity.observe(playerViewModel.fromNotification) {
        if (it) bottomPlayerBehavior.state = STATE_EXPANDED
    }
    playerBinding.playerClose.setOnClickListener {
        bottomPlayerBehavior.state = STATE_HIDDEN
    }
    playerBinding.collapsePlayer.setOnClickListener {
        bottomPlayerBehavior.state = STATE_COLLAPSED
    }

    //Connect the UI to the ViewModel

    fun <T> MutableSharedFlow<T>.emit(block: () -> T) {
        activity.emit(this, block)
    }

    val playPauseListener = object : OnCheckedStateChangedListener {
        var enabled = true
        override fun onCheckedStateChangedListener(checkBox: MaterialCheckBox, state: Int) {
            if (enabled) playerViewModel.playPause.emit {
                when (state) {
                    STATE_CHECKED -> true
                    else -> false
                }
            }
        }
    }

    playerBinding.trackPlayPause.addOnCheckedStateChangedListener(playPauseListener)
    playerBinding.collapsedTrackPlayPause.addOnCheckedStateChangedListener(playPauseListener)

    playerBinding.trackNext.setOnClickListener {
        playerViewModel.seekToNext.emit {}
        (playerBinding.trackNext.icon as Animatable).start()
    }

    playerBinding.trackPrevious.setOnClickListener {
        playerViewModel.seekToPrevious.emit {}
        (playerBinding.trackPrevious.icon as Animatable).start()
    }

    var expandedAnimator: ObjectAnimator? = null
    var collapsedAnimator: ObjectAnimator? = null
    playerBinding.expandedSeekBar.apply {
        addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                playerBinding.trackCurrentTime.text = value.toLong().toTimeString()
            }
        }
        addOnSliderTouchListener(object : OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                expandedAnimator?.cancel()
            }

            override fun onStopTrackingTouch(slider: Slider) {
                playerViewModel.seekTo.emit { slider.value.toLong() }
            }
        })
    }


    val drawables = listOf(
        AppCompatResources.getDrawable(activity, R.drawable.ic_repeat_to_repeat_one_40dp),
        AppCompatResources.getDrawable(activity, R.drawable.ic_repeat_one_to_no_repeat_40dp),
        AppCompatResources.getDrawable(activity, R.drawable.ic_no_repeat_to_repeat_40dp)
    )
    val repeatModes = listOf(
        REPEAT_MODE_ONE, REPEAT_MODE_OFF, REPEAT_MODE_ALL
    )

    playerBinding.trackRepeat.setOnClickListener {
        playerBinding.trackRepeat.icon = when (playerBinding.trackRepeat.icon) {
            drawables[0] -> drawables[1].apply {
                ObjectAnimator.ofFloat(it, "alpha", 1f, 0.4f).setDuration(400).start()
            }

            drawables[1] -> drawables[2].apply {
                ObjectAnimator.ofFloat(it, "alpha", 0.4f, 1f).setDuration(400).start()
            }

            else -> drawables[0]
        }
        (playerBinding.trackRepeat.icon as Animatable).start()
        playerViewModel.repeat.emit {
            repeatModes[drawables.indexOf(playerBinding.trackRepeat.icon)]
        }
    }

    val repeatMode = uiViewModel.repeatMode
    playerBinding.trackRepeat.icon = drawables[repeatModes.indexOf(repeatMode)]
    playerBinding.trackRepeat.alpha = if (repeatMode == REPEAT_MODE_OFF) 0.4f else 1f
    (playerBinding.trackRepeat.icon as Animatable).start()


    val linearLayoutManager = LinearLayoutManager(activity, VERTICAL, false)
    var adapter: PlaylistAdapter? = null
    val callback = object : ItemTouchHelper.SimpleCallback(UP or DOWN, START) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val new = viewHolder.bindingAdapterPosition
            val old = target.bindingAdapterPosition
            playerViewModel.moveQueueItems(new, old)
            adapter?.notifyItemMoved(new, old)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val pos = viewHolder.bindingAdapterPosition
            playerViewModel.removeQueueItem(pos)
            adapter?.notifyItemRemoved(pos)
        }
    }
    val touchHelper = ItemTouchHelper(callback)
    adapter = PlaylistAdapter(object : PlaylistAdapter.Callback() {
        override fun onDragHandleTouched(viewHolder: PlaylistAdapter.ViewHolder) {
            touchHelper.startDrag(viewHolder)
        }

        override fun onItemClicked(position: Int) {
            playerViewModel.audioIndexFlow.emit { position }
        }

        override fun onItemClosedClicked(position: Int) {
            playerViewModel.removeQueueItem(position)
            adapter?.notifyItemRemoved(position)
        }
    })

    playlistBinding.playlistRecycler.apply {
        layoutManager = linearLayoutManager
        this.adapter = adapter
        touchHelper.attachToRecyclerView(this)
    }

    playlistBinding.playlistClear.setOnClickListener {
        playerViewModel.clearQueue()
    }

    activity.apply {

        observe(uiViewModel.track) { track ->
            track ?: return@observe

            playerBinding.collapsedTrackTitle.text = track.title
            playerBinding.expandedTrackTitle.text = track.title

            track.artists.joinToString(" ") { it.name }.run {
                playerBinding.collapsedTrackAuthor.text = this
                playerBinding.expandedTrackAuthor.text = this
            }
            track.cover.run {
                loadInto(playerBinding.collapsedTrackCover, R.drawable.art_music)
                loadInto(playerBinding.expandedTrackCover, R.drawable.art_music)

                //Incomplete Dynamic Colors for Bottom Player
//                lifecycleScope.launch(Dispatchers.IO) {
//                    val req = createRequest(activity).allowHardware(false).build()
//                    val result =
//                        (imageLoader.execute(req) as? SuccessResult)?.drawable ?: return@launch
//                    val bitmap = (result as BitmapDrawable).bitmap
//
//                    launch(Dispatchers.Main) {
//                    }
//                }

            }

            container.post {
                if (bottomPlayerBehavior.state == STATE_HIDDEN) {
                    bottomPlayerBehavior.isHideable = false
                    bottomPlayerBehavior.isDraggable = true
                    bottomPlayerBehavior.state = STATE_COLLAPSED
                    bottomPlaylistBehavior.state = STATE_COLLAPSED
                }
            }
        }

        observe(uiViewModel.nextEnabled) {
            playerBinding.trackNext.isEnabled = it
        }
        observe(uiViewModel.previousEnabled) {
            playerBinding.trackPrevious.isEnabled = it
        }

        observe(uiViewModel.isPlaying) {
            playPauseListener.enabled = false
            playerBinding.trackPlayPause.isChecked = it
            playerBinding.collapsedTrackPlayPause.isChecked = it
            playPauseListener.enabled = true
        }

        observe(uiViewModel.buffering) {
            playerBinding.collapsedSeekBar.isIndeterminate = it
            playerBinding.expandedSeekBar.isEnabled = !it
            playerBinding.trackPlayPause.isEnabled = !it
            playerBinding.collapsedTrackPlayPause.isEnabled = !it
        }

        observe(uiViewModel.totalDuration) {
            playerBinding.collapsedSeekBar.max = it
            playerBinding.collapsedSeekBarBuffer.max = it
            playerBinding.expandedSeekBarBuffer.max = it
            expandedAnimator?.cancel()
            playerBinding.expandedSeekBar.apply {
                value = min(value, it.toFloat())
                valueTo = 1f + it
            }
            playerBinding.trackTotalTime.text = it.toLong().toTimeString()
        }

        fun View.getAnimator(property: String, value: Float) =
            ObjectAnimator.ofFloat(this, property, value)

        fun View.getAnimator(property: String, value: Int) =
            ObjectAnimator.ofInt(this, property, value)

        val interpolator = LinearInterpolator()
        fun View.startAnimation(property: String, value: Number, duration: Long): ObjectAnimator {
            val animator = when (value) {
                is Float -> getAnimator(property, value)
                is Int -> getAnimator(property, value)
                else -> throw IllegalStateException()
            }
            animator.interpolator = interpolator
            animator.duration = duration
            animator.start()
            return animator
        }

        observe(uiViewModel.progress) { (current, buffered) ->
            if (!playerBinding.expandedSeekBar.isPressed) {
                playerBinding.collapsedSeekBarBuffer.progress = buffered
                playerBinding.expandedSeekBarBuffer.progress = buffered

                var old = playerBinding.expandedSeekBar.value
                if (old == 0f) old = current.toFloat()
                val duration = min(1000L, max(0L, (current - old).toLong()))

                playerBinding.collapsedSeekBar.apply {
                    collapsedAnimator?.cancel()
                    collapsedAnimator = startAnimation("progress", current, duration)
                }
                playerBinding.expandedSeekBar.apply {
                    expandedAnimator?.cancel()
                    val curr = min(current.toFloat(), valueTo)
                    expandedAnimator = startAnimation("value", curr, duration)
                }
                playerBinding.trackCurrentTime.text = current.toLong().toTimeString()
            }
        }
        observe(uiViewModel.playlist) {
            adapter.setCurrent(Global.queue, it)
        }

        observe(Global.addTrackFlow) { (index, _) ->
            adapter.addItem(Global.queue, index)
        }

        observe(Global.clearQueueFlow) {
            adapter.removeItems(Global.queue)
            PlayerBackButtonHelper.playlistState.value = STATE_COLLAPSED
            PlayerBackButtonHelper.playerSheetState.value = STATE_HIDDEN
            container.post {
                if (bottomPlayerBehavior.state != STATE_HIDDEN) {
                    bottomPlayerBehavior.isHideable = true
                    bottomPlayerBehavior.state = STATE_HIDDEN
                }
            }
        }
    }
}