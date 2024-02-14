package dev.brahmkshatriya.echo.ui.player

import android.animation.ObjectAnimator
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.session.MediaController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.checkbox.MaterialCheckBox.OnCheckedStateChangedListener
import com.google.android.material.checkbox.MaterialCheckBox.STATE_CHECKED
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.ui.player.PlayerHelper.Companion.mediaItemBuilder
import dev.brahmkshatriya.echo.ui.player.PlayerHelper.Companion.toTimeString
import dev.brahmkshatriya.echo.ui.utils.dpToPx
import dev.brahmkshatriya.echo.ui.utils.emit
import dev.brahmkshatriya.echo.ui.utils.loadInto
import dev.brahmkshatriya.echo.ui.utils.observe
import dev.brahmkshatriya.echo.ui.utils.updatePaddingWithSystemInsets
import kotlinx.coroutines.flow.MutableSharedFlow

fun initPlayer(
    activity: MainActivity,
    player: MediaController
) {
    val playerBinding = activity.binding.bottomPlayer
    val container = activity.binding.bottomPlayerContainer as View

    val playerViewModel: PlayerViewModel by activity.viewModels()
    val uiViewModel: PlayerUIViewModel by activity.viewModels()


    // Apply the UI Changes

    updatePaddingWithSystemInsets(playerBinding.expandedContainer, false)
    playerBinding.expandedContainer.requestApplyInsets()

    val bottomBehavior = BottomSheetBehavior.from(container)
    container.setOnClickListener {
        bottomBehavior.state = STATE_EXPANDED
    }

    val navView = activity.binding.navView
    val bottomNavHeight =
        if (navView !is BottomNavigationView) {
            val height = Resources.getSystem().displayMetrics.heightPixels
            val frame = Rect()
            activity.window.decorView.getWindowVisibleDisplayFrame(frame)
            -(height + frame.top)
        } else 140.dpToPx()
    val collapsedCoverSize =
        activity.resources.getDimension(R.dimen.collapsed_cover_size).toInt()

    bottomBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            PlayerBackButtonHelper.playerCollapsed.value = (newState == STATE_COLLAPSED)
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            playerBinding.collapsedContainer.translationY =
                -collapsedCoverSize * slideOffset
            playerBinding.expandedContainer.translationY =
                collapsedCoverSize * (1 - slideOffset)

            navView.translationY = bottomNavHeight * slideOffset
        }
    })

    PlayerBackButtonHelper.bottomSheetBehavior = bottomBehavior

    container.post {
        bottomBehavior.state = PlayerBackButtonHelper.playerCollapsed.value.let {
            if (it) STATE_COLLAPSED else STATE_EXPANDED
        }
    }
    activity.observe(activity.fromNotification) {
        bottomBehavior.state = STATE_EXPANDED
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

    playerBinding.expandedSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            playerBinding.trackCurrentTime.text = p1.toLong().toTimeString()
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {
            p0?.progress?.let {
                playerViewModel.seekTo.emit { it.toLong() }
            }
        }
    })

    val drawables = listOf(
        AppCompatResources.getDrawable(activity, R.drawable.ic_repeat_to_repeat_one_40dp),
        AppCompatResources.getDrawable(activity, R.drawable.ic_repeat_one_to_no_repeat_40dp),
        AppCompatResources.getDrawable(activity, R.drawable.ic_no_repeat_to_repeat_40dp)
    )
    val repeatModes = listOf(
        REPEAT_MODE_ONE,
        REPEAT_MODE_OFF,
        REPEAT_MODE_ALL
    )

    val repeatMode = player.repeatMode
    playerBinding.trackRepeat.icon = drawables[repeatModes.indexOf(repeatMode)]
    playerBinding.trackRepeat.alpha = if(repeatMode == REPEAT_MODE_OFF) 0.4f else 1f
    (playerBinding.trackRepeat.icon as Animatable).start()

    playerBinding.trackRepeat.setOnClickListener {
        playerBinding.trackRepeat.icon = when (playerBinding.trackRepeat.icon) {
            drawables[0] -> drawables[1].apply {
                ObjectAnimator.ofFloat(it, "alpha", 1f, 0.4f)
                    .setDuration(400).start()
            }

            drawables[1] -> drawables[2].apply {
                ObjectAnimator.ofFloat(it, "alpha", 0.4f, 1f)
                    .setDuration(400).start()
            }

            else -> drawables[0]
        }
        (playerBinding.trackRepeat.icon as Animatable).start()
        playerViewModel.repeat.emit {
            repeatModes[drawables.indexOf(playerBinding.trackRepeat.icon)]
        }
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
            track.cover?.run {
                loadInto(playerBinding.collapsedTrackCover)
                loadInto(playerBinding.expandedTrackCover)
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
            playerBinding.expandedSeekBar.max = it

            playerBinding.trackTotalTime.text = it.toLong().toTimeString()
        }

        observe(uiViewModel.progress) { (current, buffered) ->
            if (!playerBinding.expandedSeekBar.isPressed) {
                playerBinding.collapsedSeekBar.progress = current
                playerBinding.collapsedSeekBar.secondaryProgress = buffered

                playerBinding.expandedSeekBar.secondaryProgress = buffered
                playerBinding.expandedSeekBar.progress = current
            }
        }
    }


    //Connect the ViewModel to the Player

    val listener = uiViewModel.getListener(player)
    player.addListener(listener)
    player.currentMediaItem?.let {
        listener.update(it.mediaId)
    }

    activity.apply {
        observe(playerViewModel.playPause) {
            if (it) player.play() else player.pause()
        }
        observe(playerViewModel.seekToPrevious) {
            player.seekToPrevious()
        }
        observe(playerViewModel.seekToNext) {
            player.seekToNext()
        }
        observe(playerViewModel.audioIndexFlow) {
            player.seekToDefaultPosition(it)
        }
        observe(playerViewModel.seekTo) {
            player.seekTo(it)
        }
        observe(playerViewModel.repeat) {
            player.repeatMode = it
        }
        observe(playerViewModel.audioQueueFlow) {
            val item = mediaItemBuilder(it.track, it.audio)
            player.addMediaItem(item)
            player.prepare()
            player.playWhenReady = true
        }
    }
}