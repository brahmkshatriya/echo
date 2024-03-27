package dev.brahmkshatriya.echo.player.ui

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.drawable.Animatable
import android.util.TypedValue
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.navigation.fragment.NavHostFragment
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.START
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
import dev.brahmkshatriya.echo.NavigationDirections
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.player.PlayerHelper.Companion.toTimeString
import dev.brahmkshatriya.echo.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.adapters.PlaylistAdapter
import dev.brahmkshatriya.echo.ui.settings.LookPreference.Companion.DYNAMIC_PLAYER
import dev.brahmkshatriya.echo.utils.createRequest
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.emit
import dev.brahmkshatriya.echo.utils.isNightMode
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.tryWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


fun createPlayerUI(
    activity: MainActivity
) {

    val preferences = activity.preferences

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

    val linearLayoutManager = LinearLayoutManager(activity, VERTICAL, false)

    bottomPlaylistBehavior.addBottomSheetCallback(object :
        BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            bottomPlayerBehavior.isDraggable = newState == STATE_COLLAPSED

            if (newState == STATE_SETTLING || newState == STATE_DRAGGING) return
            playerBinding.expandedSeekBar.isEnabled = newState != STATE_EXPANDED
            linearLayoutManager.scrollToPositionWithOffset(uiViewModel.currentIndex.value ?: 0, 0)
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
                playerViewModel.clearQueue()
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            val offset = max(0f, slideOffset)
            playerBinding.collapsedContainer.translationY = -collapsedCoverSize * offset
            playerBinding.expandedContainer.translationY = collapsedCoverSize * (1 - offset)
            navView.translationY = uiViewModel.bottomNavTranslateY * offset
            activity.binding.snackbarContainer.translationY =
                -collapsedCoverSize * (1 - abs(slideOffset))
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
        if (it) bottomPlayerBehavior.state = STATE_EXPANDED.also {
            println("From Notification expanded")
        }
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
            drawables[0] -> drawables[1]
            drawables[1] -> drawables[2]
            else -> drawables[0]
        }
        (playerBinding.trackRepeat.icon as Animatable).start()
        playerViewModel.repeat.emit {
            repeatModes[drawables.indexOf(playerBinding.trackRepeat.icon)]
        }
    }

    val repeatMode = uiViewModel.repeatMode
    playerBinding.trackRepeat.icon = drawables[repeatModes.indexOf(repeatMode)]
    (playerBinding.trackRepeat.icon as Animatable).start()


    val callback = object : ItemTouchHelper.SimpleCallback(UP or DOWN, START) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val new = viewHolder.bindingAdapterPosition
            val old = target.bindingAdapterPosition
            playerViewModel.moveQueueItems(new, old)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val pos = viewHolder.bindingAdapterPosition
            playerViewModel.removeQueueItem(pos)
        }
    }
    val touchHelper = ItemTouchHelper(callback)
    val adapter = PlaylistAdapter(object : PlaylistAdapter.Callback() {
        override fun onDragHandleTouched(viewHolder: PlaylistAdapter.ViewHolder) {
            touchHelper.startDrag(viewHolder)
        }

        override fun onItemClicked(position: Int) {
            playerViewModel.audioIndexFlow.emit { position }
        }

        override fun onItemClosedClicked(position: Int) {
            playerViewModel.removeQueueItem(position)
        }
    })
    adapter.submitList(uiViewModel.list)

    playlistBinding.playlistRecycler.apply {
        layoutManager = linearLayoutManager
        this.adapter = adapter
        touchHelper.attachToRecyclerView(this)
    }

    playlistBinding.playlistClear.setOnClickListener {
        playerViewModel.clearQueue()
    }

    playlistBinding.playlistShuffle.apply {
        val stroke = 1.dpToPx()
        strokeWidth = if (uiViewModel.shuffled.value) stroke else 0
        setOnClickListener {
            playerViewModel.shuffle(!uiViewModel.shuffled.value)
        }
    }

    uiViewModel.view = WeakReference(playerBinding.collapsedTrackCover)

    data class PlayerColors(
        val background: Int,
        val clickable: Int,
        val body: Int,
    )

    fun applyColors(colors: PlayerColors) {
        playerBinding.root.setBackgroundColor(colors.background)
        playerBinding.collapsedContainer.setBackgroundColor(colors.background)

        playerBinding.expandedTrackAuthor.setTextColor(colors.clickable)
        val clickableState = ColorStateList.valueOf(colors.clickable)

        playerBinding.expandedSeekBar.trackActiveTintList = clickableState
        playerBinding.expandedSeekBar.thumbTintList = clickableState
        playerBinding.expandedProgressBar.setIndicatorColor(colors.clickable)
        playerBinding.collapsedProgressBar.setIndicatorColor(colors.clickable)
        playerBinding.collapsedSeekBar.setIndicatorColor(colors.clickable)

        playerBinding.expandedSeekBarBuffer.setIndicatorColor(colors.clickable)
        playerBinding.collapsedSeekBarBuffer.setIndicatorColor(colors.clickable)

        playerBinding.expandedSeekBarBuffer.trackColor = colors.body
        playerBinding.collapsedSeekBarBuffer.trackColor = colors.body

        playerBinding.trackCurrentTime.setTextColor(colors.body)
        playerBinding.trackTotalTime.setTextColor(colors.body)
        playerBinding.expandedTrackTitle.setTextColor(colors.body)

        playerBinding.collapsedTrackAuthor.setTextColor(colors.body)
        playerBinding.collapsedTrackTitle.setTextColor(colors.body)
    }

    activity.apply {
        val navController = binding.navHostFragment
            .getFragment<NavHostFragment>().navController

        val background = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, background, true)
        val tertiary = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorTertiary, tertiary, true)
        val onSurface = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, onSurface, true)

        val defaultColors = PlayerColors(
            background.data,
            tertiary.data,
            onSurface.data
        )

        observe(uiViewModel.track) { track ->
            track ?: return@observe

            playerBinding.collapsedTrackTitle.text = track.title
            playerBinding.expandedTrackTitle.text = track.title

            track.artists.joinToString(" ") { it.name }.run {
                playerBinding.collapsedTrackAuthor.text = this
                playerBinding.expandedTrackAuthor.text = this
            }


            playerBinding.expandedTrackAuthor.setOnClickListener {
                val artist = track.artists.firstOrNull() ?: return@setOnClickListener
                val action = NavigationDirections.actionArtist(artist)
                bottomPlayerBehavior.state = STATE_COLLAPSED
                navController.navigate(action)
            }

            track.cover.run {
                loadInto(playerBinding.collapsedTrackCover, R.drawable.art_music)
                loadInto(playerBinding.expandedTrackCover, R.drawable.art_music)

                lifecycleScope.launch(Dispatchers.IO) {
                    val dynamicPlayer = preferences.getBoolean(DYNAMIC_PLAYER, true)
                    val req = if (dynamicPlayer) {
                        val builder = Glide.with(activity).asBitmap()
                        createRequest(builder).submit()
                    } else null
                    val bitmap = tryWith { req?.get() }
                    val palette = bitmap?.let { Palette.from(it).generate() }

                    val colors = palette?.run {
                        val lightMode = !activity.isNightMode()
                        val lightSwatch = lightVibrantSwatch
                            ?: lightMutedSwatch ?: vibrantSwatch
                        val darkSwatch = darkVibrantSwatch
                            ?: darkMutedSwatch ?: mutedSwatch

                        val bgSwatch = if (lightMode) lightSwatch else darkSwatch
                        bgSwatch?.run {
                            val clickSwatch = if (lightMode) darkSwatch else lightSwatch
                            PlayerColors(rgb, clickSwatch?.rgb ?: titleTextColor, bodyTextColor)
                        }
                    } ?: defaultColors

                    launch(Dispatchers.Main) {
                        applyColors(colors)
                    }
                }

            }

            container.post {
                if (bottomPlayerBehavior.state == STATE_HIDDEN) {
                    bottomPlayerBehavior.state = STATE_COLLAPSED
                    bottomPlaylistBehavior.state = STATE_COLLAPSED
                    bottomPlayerBehavior.isDraggable = true
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
            playerBinding.collapsedProgressBar.isVisible = it
            playerBinding.expandedProgressBar.isVisible = it

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
        observe(uiViewModel.shuffled) {
            val stroke = 1.dpToPx()
            playlistBinding.playlistShuffle.strokeWidth = if (it) stroke else 0
        }

        observe(uiViewModel.currentIndex) {
            linearLayoutManager.scrollToPositionWithOffset(it ?: 0, 0)
        }

        observe(uiViewModel.listChangeFlow) {
            adapter.submitList(uiViewModel.list)
            if (uiViewModel.list.isEmpty()) {
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
}