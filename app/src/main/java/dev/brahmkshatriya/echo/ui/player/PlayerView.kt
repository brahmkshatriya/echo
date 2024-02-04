package dev.brahmkshatriya.echo.ui.player

import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.Listener
import androidx.media3.session.MediaController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.checkbox.MaterialCheckBox.STATE_CHECKED
import com.google.android.material.checkbox.MaterialCheckBox.STATE_UNCHECKED
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.data.models.StreamableAudio
import dev.brahmkshatriya.echo.databinding.BottomPlayerBinding
import dev.brahmkshatriya.echo.ui.utils.dpToPx
import dev.brahmkshatriya.echo.ui.utils.loadInto
import dev.brahmkshatriya.echo.ui.utils.updatePaddingWithSystemInsets
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlayerView(
    private val activity: MainActivity,
    player: MediaController,
    private val view: View,
    private val binding: BottomPlayerBinding
) {

    val viewModel by activity.viewModels<PlayerViewModel>()

    init {
        applyView()
        connect(player)
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
                viewModel.playerCollapsed.value = (newState == STATE_COLLAPSED)
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

        viewModel.bottomSheetBehavior = bottomBehavior

        view.post {
            bottomBehavior.state = viewModel.playerCollapsed.value.let {
                if (it) STATE_COLLAPSED else STATE_EXPANDED
            }
        }
    }

    private fun connect(player: MediaController) {
        binding.trackPlayPause.addOnCheckedStateChangedListener { _, state ->
            when (state) {
                STATE_CHECKED -> player.play()
                STATE_UNCHECKED -> player.pause()
            }
        }
        binding.collapsedTrackPlayPause.addOnCheckedStateChangedListener { _, state ->
            when (state) {
                STATE_CHECKED -> player.play()
                STATE_UNCHECKED -> player.pause()
            }
        }
        player.addListener(object : Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_IDLE, Player.STATE_BUFFERING -> {
                        binding.trackPlayPause.isEnabled = false
                        binding.collapsedTrackPlayPause.isEnabled = false
                    }

                    Player.STATE_READY, Player.STATE_ENDED -> {
                        binding.trackPlayPause.isEnabled = true
                        binding.collapsedTrackPlayPause.isEnabled = true
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                binding.trackPlayPause.isEnabled = false
                binding.collapsedTrackPlayPause.isEnabled = false

                binding.trackPlayPause.isChecked = isPlaying
                binding.collapsedTrackPlayPause.isChecked = isPlaying

                binding.trackPlayPause.isEnabled = true
                binding.collapsedTrackPlayPause.isEnabled = true
            }

        })

        activity.lifecycleScope.launch {
            viewModel.audioFlow.collectLatest {
                it?.run {
                    val track = this.first

                    binding.collapsedTrackTitle.text = track.title
                    binding.expandedTrackTitle.text = track.title

                    track.artists.joinToString { " " }.run {
                        binding.collapsedTrackAuthor.text = this
                        binding.expandedTrackAuthor.text = this
                    }
                    track.cover?.run {
                        loadInto(binding.collapsedTrackCover)
                        loadInto(binding.expandedTrackCover)
                    }

                    val item = when (val audio = this.second) {
                        is StreamableAudio.StreamableFile -> {
                            MediaItem.fromUri(audio.uri)
                        }

                        is StreamableAudio.ByteStreamAudio -> TODO()
                        is StreamableAudio.StreamableUrl -> {
                            MediaItem.Builder()
                                .setUri(audio.url.url)
                                .build()
                        }
                    }
                    player.setMediaItem(item)
                    player.prepare()
                    player.play()
                }

            }
        }
    }
}