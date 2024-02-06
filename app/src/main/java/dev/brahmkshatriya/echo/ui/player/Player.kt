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
import dev.brahmkshatriya.echo.data.models.StreamableAudio
import dev.brahmkshatriya.echo.databinding.BottomPlayerBinding
import dev.brahmkshatriya.echo.ui.player.PlayerHelper.Companion.toMetaData
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
            player.seekToNextMediaItem()
        }

        binding.trackPrevious.setOnClickListener {
            player.seekToPreviousMediaItem()
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

        activity.lifecycleScope.launch {
            viewModel.audioFlow.collectLatest { pair ->
                pair?.run {
                    val track = this.first
                    val metadata = track.toMetaData()
                    listener.map(metadata, track)

                    val builder = MediaItem.Builder()
                        .setMediaMetadata(metadata)
                    val item = when (val audio = this.second) {
                        is StreamableAudio.StreamableFile -> {
                            builder.setUri(audio.uri)
                        }

                        is StreamableAudio.StreamableUrl -> {
                            builder.setUri(audio.url.url)
                        }

                        is StreamableAudio.ByteStreamAudio -> TODO()
                    }
                    player.setMediaItem(item.build())
                    player.prepare()
                    player.play()
                }

            }
        }
    }
}