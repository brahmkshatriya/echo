package dev.brahmkshatriya.echo.ui.player

import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.checkbox.MaterialCheckBox.OnCheckedStateChangedListener
import com.google.android.material.checkbox.MaterialCheckBox.STATE_CHECKED
import com.google.android.material.checkbox.MaterialCheckBox.STATE_UNCHECKED
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.ui.player.PlayerHelper.Companion.mediaItemBuilder
import dev.brahmkshatriya.echo.ui.player.PlayerHelper.Companion.toTimeString
import dev.brahmkshatriya.echo.ui.utils.collect
import dev.brahmkshatriya.echo.ui.utils.collectAndRemove
import dev.brahmkshatriya.echo.ui.utils.dpToPx
import dev.brahmkshatriya.echo.ui.utils.loadInto
import dev.brahmkshatriya.echo.ui.utils.updatePaddingWithSystemInsets
import kotlinx.coroutines.launch

class Player(
    private val activity: MainActivity,
    private val player: MediaController
) {
    private val binding = activity.binding.bottomPlayer
    private val container = activity.binding.bottomPlayerContainer as View

    private val playerViewModel: PlayerViewModel by activity.viewModels()
    private val uiViewModel: PlayerUIViewModel by activity.viewModels()

    init {
        applyViewChanges()
        connectUiToViewModel()
        connectPlayerToViewModel()
    }

    private fun applyViewChanges() {

        updatePaddingWithSystemInsets(binding.expandedContainer, false)
        binding.expandedContainer.requestApplyInsets()
        
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
            } else
                140.dpToPx()
        val collapsedCoverSize =
            activity.resources.getDimension(R.dimen.collapsed_cover_size).toInt()

        bottomBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                PlayerBackButtonHelper.playerCollapsed.value = (newState == STATE_COLLAPSED)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.collapsedContainer.translationY = -collapsedCoverSize * slideOffset
                binding.expandedContainer.translationY = collapsedCoverSize * (1 - slideOffset)

                navView.translationY = bottomNavHeight * slideOffset
            }
        })

        PlayerBackButtonHelper.bottomSheetBehavior = bottomBehavior

        container.post {
            bottomBehavior.state = PlayerBackButtonHelper.playerCollapsed.value.let {
                if (it) STATE_COLLAPSED else STATE_EXPANDED
            }
        }
    }

    private fun connectUiToViewModel() {
        val playPauseListener = OnCheckedStateChangedListener { _, state ->
            playerViewModel.playPause.value = when (state) {
                STATE_CHECKED -> true
                STATE_UNCHECKED -> false
                else -> null
            }
        }

        binding.trackPlayPause.addOnCheckedStateChangedListener(playPauseListener)
        binding.collapsedTrackPlayPause.addOnCheckedStateChangedListener(playPauseListener)

        binding.trackNext.setOnClickListener {
            playerViewModel.seekToNext.value = Unit
        }

        binding.trackPrevious.setOnClickListener {
            playerViewModel.seekToPrevious.value = Unit
        }


        binding.expandedSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                binding.trackCurrentTime.text = p1.toLong().toTimeString()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {
                p0?.progress?.let {
                    playerViewModel.seekTo.value = it.toLong()
                }
            }
        })


        activity.lifecycleScope.launch {
            collect(uiViewModel.track) { track ->
                track ?: return@collect

                binding.collapsedTrackTitle.text = track.title
                binding.expandedTrackTitle.text = track.title

                track.artists.joinToString(" ") { it.name }.run {
                    binding.collapsedTrackAuthor.text = this
                    binding.expandedTrackAuthor.text = this
                }
                track.cover?.run {
                    loadInto(binding.collapsedTrackCover)
                    loadInto(binding.expandedTrackCover)
                }
            }

            collect(uiViewModel.nextEnabled) {
                binding.trackNext.isEnabled = it
            }
            collect(uiViewModel.previousEnabled) {
                binding.trackPrevious.isEnabled = it
            }

            collect(uiViewModel.isPlaying) {
                binding.trackPlayPause
                    .removeOnCheckedStateChangedListener(playPauseListener)
                binding.collapsedTrackPlayPause
                    .removeOnCheckedStateChangedListener(playPauseListener)

                binding.trackPlayPause.isChecked = it
                binding.collapsedTrackPlayPause.isChecked = it

                binding.trackPlayPause
                    .addOnCheckedStateChangedListener(playPauseListener)
                binding.collapsedTrackPlayPause
                    .addOnCheckedStateChangedListener(playPauseListener)
            }

            collect(uiViewModel.buffering) {
                binding.collapsedSeekBar.isIndeterminate = it
                binding.expandedSeekBar.isEnabled = !it
                binding.trackPlayPause.isEnabled = !it
                binding.collapsedTrackPlayPause.isEnabled = !it
            }
            collect(uiViewModel.progress) { (current, buffered) ->
                if (!binding.expandedSeekBar.isPressed) {
                    binding.collapsedSeekBar.progress = current
                    binding.collapsedSeekBar.secondaryProgress = buffered

                    binding.expandedSeekBar.secondaryProgress = buffered
                    binding.expandedSeekBar.progress = current
                }
            }
            collect(uiViewModel.totalDuration) {
                binding.collapsedSeekBar.max = it
                binding.expandedSeekBar.max = it

                binding.trackTotalTime.text = it.toLong().toTimeString()
            }
        }
    }

    private fun connectPlayerToViewModel() {

        val listener = uiViewModel.getListener(player)
        player.addListener(listener)

        activity.lifecycleScope.launch {
            collectAndRemove(playerViewModel.playPause) {
                if (it) player.play() else player.pause()
            }
            collectAndRemove(playerViewModel.seekToPrevious) {
                player.seekToPrevious()
            }
            collectAndRemove(playerViewModel.seekToNext) {
                player.seekToNext()
            }
            collectAndRemove(playerViewModel.audioIndexFlow) {
                player.seekToDefaultPosition(it)
            }
            collectAndRemove(playerViewModel.seekTo){
                player.seekTo(it)
            }
            collectAndRemove(playerViewModel.audioQueueFlow) {
                val item = mediaItemBuilder(it.track, it.audio)
                listener.map[item.mediaMetadata] = it.track
                player.addMediaItem(item)
                player.prepare()
                player.playWhenReady = true
            }
        }

    }
}