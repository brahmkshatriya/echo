package dev.brahmkshatriya.echo.ui.player

import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
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
import dev.brahmkshatriya.echo.databinding.BottomPlayerBinding
import dev.brahmkshatriya.echo.ui.player.PlayerHelper.Companion.toTimeString
import dev.brahmkshatriya.echo.ui.utils.dpToPx
import dev.brahmkshatriya.echo.ui.utils.updatePaddingWithSystemInsets
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class Player(
    private val activity: MainActivity,
    private val player: MediaController,
    private val view: View,
    private val binding: BottomPlayerBinding
) {

    private val viewModel: PlayerViewModel by activity.viewModels()

    init {
        applyView()
        connect()
    }

    private fun applyView() {

        updatePaddingWithSystemInsets(binding.expandedContainer, false)
        view.setOnClickListener {
            BottomSheetBehavior.from(view).state = STATE_EXPANDED
        }

        val bottomBehavior = BottomSheetBehavior.from(view)
        val navView = activity.navView
        val bottomNavHeight = 140.dpToPx()
        val collapsedCoverSize =
            activity.resources.getDimension(R.dimen.collapsed_cover_size).toInt()

        bottomBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                PlayerBackButtonHelper.playerCollapsed.value = (newState == STATE_COLLAPSED)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.collapsedContainer.translationY = -collapsedCoverSize * slideOffset
                binding.expandedContainer.translationY = collapsedCoverSize * (1 - slideOffset)

                if (navView is BottomNavigationView)
                    navView.translationY = bottomNavHeight * slideOffset
                else
                    navView.translationX = -bottomNavHeight * slideOffset
            }
        })

        PlayerBackButtonHelper.bottomSheetBehavior = bottomBehavior

        view.post {
            bottomBehavior.state = PlayerBackButtonHelper.playerCollapsed.value.let {
                if (it) STATE_COLLAPSED else STATE_EXPANDED
            }
        }
    }

    private fun connect() {
        val playPauseListener = OnCheckedStateChangedListener { _, state ->
            when (state) {
                STATE_CHECKED -> player.play()
                STATE_UNCHECKED -> player.pause()
            }
        }

        binding.trackPlayPause.addOnCheckedStateChangedListener(playPauseListener)
        binding.collapsedTrackPlayPause.addOnCheckedStateChangedListener(playPauseListener)

        binding.trackNext.setOnClickListener {
            player.seekToNext()
        }

        binding.trackPrevious.setOnClickListener {
            player.seekToPrevious()
        }


        binding.expandedSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                binding.trackCurrentTime.text = p1.toLong().toTimeString()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {
                p0?.progress?.let {
                    player.seekTo(it.toLong())
                    binding.trackCurrentTime.text = it.toLong().toTimeString()
                }
            }
        })

        val listener = PlayerListener(player, binding, playPauseListener)
        player.addListener(listener)

        fun addToQueue(it: TrackWithStream?): MediaItem? {
            it ?: return null
            val item = it.mediaItemBuilder()
            listener.map[item.mediaMetadata] = it.track
            player.addMediaItem(item)
            player.prepare()
            player.play()
            return item
        }

        activity.lifecycleScope.launch {
            launch {
                viewModel.audioFlow.collectLatest {
//                    val item = addToQueue(it) ?: return@collectLatest
                }
            }
            launch {
                viewModel.audioQueueFlow.collectLatest {
                    addToQueue(it)
                }
            }
        }
    }
}