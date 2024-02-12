package dev.brahmkshatriya.echo.ui.player

import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.activity.viewModels
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

class Player(
    private val activity: MainActivity,
    private val player: MediaController
) {
    private val playerBinding = activity.binding.bottomPlayer
    private val container = activity.binding.bottomPlayerContainer as View

    private val playerViewModel: PlayerViewModel by activity.viewModels()
    private val uiViewModel: PlayerUIViewModel by activity.viewModels()

    init {
        applyViewChanges()
        connectUiToViewModel()
        connectPlayerToViewModel()
    }

    private fun applyViewChanges() {

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
    }

    private fun connectUiToViewModel() {
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
        }

        playerBinding.trackPrevious.setOnClickListener {
            playerViewModel.seekToPrevious.emit {}
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
    }

    private fun connectPlayerToViewModel() {

        val listener = uiViewModel.getListener(player)
        player.addListener(listener)

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
            observe(playerViewModel.audioQueueFlow) {
                val item = mediaItemBuilder(it.track, it.audio)
                player.addMediaItem(item)
                player.prepare()
                player.playWhenReady = true
            }
        }
    }
}