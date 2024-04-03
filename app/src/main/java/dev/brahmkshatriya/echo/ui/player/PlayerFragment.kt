package dev.brahmkshatriya.echo.ui.player

import android.content.res.ColorStateList
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentPlayerBinding
import dev.brahmkshatriya.echo.databinding.ItemPlayerControlsBinding
import dev.brahmkshatriya.echo.player.PlayerHelper.Companion.toTimeString
import dev.brahmkshatriya.echo.ui.player.PlayerColors.Companion.defaultPlayerColors
import dev.brahmkshatriya.echo.ui.player.PlayerColors.Companion.getColorsFrom
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.emit
import dev.brahmkshatriya.echo.utils.getBitmap
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.setupPlayerInfoBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.min

class PlayerFragment : Fragment() {
    private var binding by autoCleared<FragmentPlayerBinding>()
    private val viewModel by activityViewModels<PlayerViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uiViewModel by activityViewModels<UiViewModel>()
        setupPlayerInfoBehavior(uiViewModel, binding.playerInfoContainer)

        val adapter = PlayerTrackAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.setPageTransformer(
            ParallaxPageTransformer(R.id.expandedTrackCoverContainer)
        )
        binding.viewPager.registerOnPageChangeCallback(changeCallback)

        binding.viewPager.getChildAt(0).run {
            this as RecyclerView
            isNestedScrollingEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        view.post {
            observe(viewModel.listChangeFlow) {
                adapter.submitList(it)
                if (it.isEmpty()) {
                    emit(uiViewModel.changeInfoState) { STATE_COLLAPSED }
                    emit(uiViewModel.changePlayerState) { STATE_HIDDEN }
                } else {
                    if (uiViewModel.playerSheetState.value == STATE_HIDDEN) {
                        emit(uiViewModel.changePlayerState) { STATE_COLLAPSED }
                        emit(uiViewModel.changeInfoState) { STATE_COLLAPSED }
                    }
                }
            }
            observe(uiViewModel.playerSheetState){
                if (it == STATE_HIDDEN) viewModel.clearQueue()
            }
        }

        observe(viewModel.currentIndex) {
            it ?: return@observe
            binding.viewPager.currentItem = it
        }

        observe(uiViewModel.playerSheetOffset) {
            binding.collapsePlayer.alpha = it
            binding.collapsePlayer.isVisible = it != 0f
        }
        binding.collapsePlayer.setOnClickListener {
            emit(uiViewModel.changePlayerState) { STATE_COLLAPSED }
        }

        observeControls(viewModel.track) { track ->
            trackTitle.text = track?.title
            trackArtist.text = track?.artists?.joinToString(", ") { it.name }
        }

        observeControls(viewModel.totalDuration) {
            bufferBar.max = it
            seekBar.apply {
                value = min(value, it.toFloat())
                valueTo = 1f + it
            }
            trackTotalTime.text = it.toLong().toTimeString()
        }
        binding.playerControls.seekBar.apply {
            addOnChangeListener { _, value, fromUser ->
                if (fromUser)
                    binding.playerControls.trackCurrentTime.text = value.toLong().toTimeString()
            }
            addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) = Unit
                override fun onStopTrackingTouch(slider: Slider) =
                    emit(viewModel.seekTo) { slider.value.toLong() }
            })
        }
        observeControls(viewModel.progress) { (current, buffered) ->
            if (!seekBar.isPressed) {
                bufferBar.progress = buffered
                seekBar.value = min(current.toFloat(), seekBar.valueTo)
                trackCurrentTime.text = current.toLong().toTimeString()
            }
        }
        observeControls(viewModel.buffering) {
            progressBar.isVisible = it
            seekBar.isEnabled = !it
            trackPlayPause.isEnabled = !it
        }

        observeControls(viewModel.nextEnabled) { trackNext.isEnabled = it }
        binding.playerControls.trackNext.setOnClickListener {
            emit(viewModel.seekToNext)
            it as MaterialButton
            (it.icon as Animatable).start()
        }

        observeControls(viewModel.previousEnabled) { trackPrevious.isEnabled = it }
        binding.playerControls.trackPrevious.setOnClickListener {
            emit(viewModel.seekToPrevious)
            it as MaterialButton
            (it.icon as Animatable).start()
        }

        val playPauseListener = viewModel.playPauseListener
        binding.playerControls.trackPlayPause.addOnCheckedStateChangedListener(playPauseListener)
        observeControls(viewModel.isPlaying) {
            playPauseListener.enabled = false
            trackPlayPause.isChecked = it
            playPauseListener.enabled = true
        }

        val shuffleListener = CheckBoxListener { emit(viewModel.shuffle) { it } }
        observeControls(viewModel.shuffle) {
            shuffleListener.enabled = false
            trackShuffle.isChecked = it
            shuffleListener.enabled = true
        }

        val drawables = getRepeatDrawables()
        var repeatEnabled = true
        binding.playerControls.trackRepeat.setOnClickListener {
            val button = binding.playerControls.trackRepeat
            val drawable = when (button.icon) {
                drawables[0] -> drawables[1]
                drawables[1] -> drawables[2]
                else -> drawables[0]
            }
            button.icon = drawable
            drawable.start()
            if (repeatEnabled)
                emit(viewModel.repeat) { repeatModes[drawables.indexOf(drawable)] }
        }

        observeControls(viewModel.repeat) {
            repeatEnabled = false
            trackRepeat.performClick()
            repeatEnabled = true
        }

        val repeatMode = viewModel.repeat.value
        binding.playerControls.trackRepeat.icon =
            drawables[repeatModes.indexOf(repeatMode)].apply { start() }
    }

    private fun <T> observeControls(flow: Flow<T>, block: ItemPlayerControlsBinding.(T) -> Unit) {
        observe(flow) { binding.playerControls.block(it) }
    }

    private val changeCallback = object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (viewModel.currentIndex.value != position) {
                emit(viewModel.audioIndexFlow) { position }
                val cover = viewModel.track.value?.cover
                lifecycleScope.launch(Dispatchers.IO) {
                    requireContext().run {
                        val bitmap = cover?.getBitmap(this)
                        val colors = getColorsFrom(bitmap) ?: defaultPlayerColors()
                        launch(Dispatchers.Main) {
                            binding.playerControls.applyColors(colors)
                        }
                    }
                }
            }
        }
    }

    private fun ItemPlayerControlsBinding.applyColors(colors: PlayerColors) {
        val clickableState = ColorStateList.valueOf(colors.clickable)
        seekBar.trackActiveTintList = clickableState
        seekBar.thumbTintList = clickableState

        progressBar.setIndicatorColor(colors.clickable)
        bufferBar.setIndicatorColor(colors.clickable)
        bufferBar.trackColor = colors.body
        trackCurrentTime.setTextColor(colors.body)
        trackTotalTime.setTextColor(colors.body)
        trackTitle.setTextColor(colors.body)
    }

    private fun getRepeatDrawables() = requireContext().run {
        fun asAnimated(id: Int) =
            AppCompatResources.getDrawable(this, id) as AnimatedVectorDrawable
        listOf(
            asAnimated(R.drawable.ic_repeat_to_repeat_one_40dp),
            asAnimated(R.drawable.ic_repeat_one_to_no_repeat_40dp),
            asAnimated(R.drawable.ic_no_repeat_to_repeat_40dp)
        )
    }

    private val repeatModes =
        listOf(Player.REPEAT_MODE_ONE, Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL)
}

