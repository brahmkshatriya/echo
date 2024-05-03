package dev.brahmkshatriya.echo.ui.player

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LibraryClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemPlayerCollapsedBinding
import dev.brahmkshatriya.echo.databinding.ItemPlayerControlsBinding
import dev.brahmkshatriya.echo.databinding.ItemPlayerTrackBinding
import dev.brahmkshatriya.echo.playback.Queue.StreamableTrack
import dev.brahmkshatriya.echo.ui.player.PlayerColors.Companion.defaultPlayerColors
import dev.brahmkshatriya.echo.ui.player.PlayerColors.Companion.getColorsFrom
import dev.brahmkshatriya.echo.ui.settings.LookFragment
import dev.brahmkshatriya.echo.utils.emit
import dev.brahmkshatriya.echo.utils.load
import dev.brahmkshatriya.echo.utils.loadBitmap
import dev.brahmkshatriya.echo.utils.loadWith
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.toTimeString
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class PlayerTrackAdapter(
    fragment: Fragment,
    private val onItemClicked: (String?, EchoMediaItem) -> Unit
) : LifeCycleListAdapter<StreamableTrack, ItemPlayerTrackBinding>(DiffCallback) {

    private val viewModel by fragment.activityViewModels<PlayerViewModel>()
    private val uiViewModel by fragment.activityViewModels<UiViewModel>()

    object DiffCallback : DiffUtil.ItemCallback<StreamableTrack>() {
        override fun areItemsTheSame(oldItem: StreamableTrack, newItem: StreamableTrack) =
            oldItem.unloaded.id == newItem.unloaded.id

        override fun areContentsTheSame(oldItem: StreamableTrack, newItem: StreamableTrack) =
            true
    }

    override fun inflateCallback(inflater: LayoutInflater, container: ViewGroup?) =
        ItemPlayerTrackBinding.inflate(inflater, container, false)

    override fun Holder<StreamableTrack, ItemPlayerTrackBinding>.onBind(position: Int) {
        val item = getItem(position)
        val client = item?.clientId
        val track = item?.loaded ?: item?.unloaded
        binding.applyTrackDetails(client, track)
        observe(item.onLoad) {
            binding.applyTrackDetails(client, it, item.unloaded)
        }

        lifecycleScope.launch {
            val bitmap = track?.cover?.loadBitmap(binding.root.context)
            val colors = binding.root.context.getPlayerColors(bitmap)
            binding.root.setBackgroundColor(colors.background)
            binding.bgGradient.imageTintList = ColorStateList.valueOf(colors.background)
            binding.expandedToolbar.run {
                setTitleTextColor(colors.text)
                setSubtitleTextColor(colors.text)
            }
            binding.collapsedContainer.applyColors(colors)
            binding.playerControls.applyColors(colors)
        }

        binding.collapsedContainer.root.setOnClickListener {
            emit(uiViewModel.changePlayerState) { BottomSheetBehavior.STATE_EXPANDED }
        }
        binding.expandedToolbar.setNavigationOnClickListener {
            emit(uiViewModel.changePlayerState) { BottomSheetBehavior.STATE_COLLAPSED }
        }
        binding.collapsedContainer.playerClose.setOnClickListener {
            emit(uiViewModel.changePlayerState) { BottomSheetBehavior.STATE_HIDDEN }
        }

        observe(uiViewModel.playerSheetOffset) {
            val offset = max(0f, it)
            val height = binding.collapsedContainer.root.height
            binding.collapsedContainer.root.run {
                translationY = -height * offset
                alpha = 1 - offset
            }
            binding.bgImage.alpha = offset
            binding.expandedTrackCoverContainer.alpha = offset
        }

        observe(uiViewModel.infoSheetOffset) {
            binding.background.alpha = it
            binding.playerControls.root.isVisible = it != 1f
        }

        observe(uiViewModel.systemInsets) {
            binding.expandedTrackCoverContainer.applyInsets(it, 24)
        }

        fun <T> observeCurrent(
            flow: Flow<T>,
            block: (T?) -> Unit
        ) {
            observe(flow) {
                if (viewModel.current?.unloaded?.id == track?.id)
                    block(it)
                else block(null)
            }
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
        observeCurrent(viewModel.progress) {
            val (current, buffered) = it ?: (0 to 0)
            binding.collapsedContainer.run {
                collapsedBuffer.progress = buffered
                collapsedSeekBar.progress = current
            }
            binding.playerControls.run {
                if (!seekBar.isPressed) {
                    bufferBar.progress = buffered
                    seekBar.value = min(current.toFloat(), seekBar.valueTo)
                    trackCurrentTime.text = current.toLong().toTimeString()
                }
            }
        }

        observe(viewModel.buffering) { buffering ->
            binding.collapsedContainer.run {
                collapsedProgressBar.isVisible = buffering
                collapsedTrackPlayPause.isEnabled = !buffering
            }
            binding.playerControls.run {
                progressBar.isVisible = buffering
                seekBar.isEnabled = !buffering
                trackPlayPause.isEnabled = !buffering
            }
        }

        observeCurrent(viewModel.totalDuration) {
            val duration = it ?: track?.duration?.toInt() ?: 100
            binding.collapsedContainer.run {
                collapsedSeekBar.max = duration
                collapsedBuffer.max = duration
            }
            binding.playerControls.run {
                bufferBar.max = duration
                seekBar.apply {
                    value = min(value, duration.toFloat())
                    valueTo = 1f + duration
                }
                trackTotalTime.text = duration.toLong().toTimeString()
            }
        }


        val playPauseListener = viewModel.playPauseListener

        binding.collapsedContainer.collapsedTrackPlayPause
            .addOnCheckedStateChangedListener(playPauseListener)
        binding.playerControls.trackPlayPause
            .addOnCheckedStateChangedListener(playPauseListener)

        observe(viewModel.isPlaying) {
            playPauseListener.enabled = false
            binding.playerControls.trackPlayPause.isChecked = it
            binding.collapsedContainer.collapsedTrackPlayPause.isChecked = it
            playPauseListener.enabled = true
        }

        observe(viewModel.nextEnabled) {
            binding.playerControls.trackNext.isEnabled = it
        }
        binding.playerControls.trackNext.setOnClickListener {
            emit(viewModel.seekToNext)
            it as MaterialButton
            (it.icon as Animatable).start()
        }
        observe(viewModel.previousEnabled) {
            binding.playerControls.trackPrevious.isEnabled = it
        }
        binding.playerControls.trackPrevious.setOnClickListener {
            emit(viewModel.seekToPrevious)
            it as MaterialButton
            (it.icon as Animatable).start()
        }

        val shuffleListener = viewModel.shuffleListener
        binding.playerControls.trackShuffle.addOnCheckedStateChangedListener(shuffleListener)
        observe(viewModel.shuffle) {
            shuffleListener.enabled = false
            binding.playerControls.trackShuffle.isChecked = it
            shuffleListener.enabled = true
        }

        val drawables = binding.root.context.run {
            fun asAnimated(id: Int) =
                AppCompatResources.getDrawable(this, id) as AnimatedVectorDrawable
            listOf(
                asAnimated(R.drawable.ic_repeat_one_to_no_repeat_40dp),
                asAnimated(R.drawable.ic_no_repeat_to_repeat_40dp),
                asAnimated(R.drawable.ic_repeat_to_repeat_one_40dp)
            )
        }
        val repeatModes = listOf(REPEAT_MODE_OFF, REPEAT_MODE_ALL, REPEAT_MODE_ONE)
        fun changeRepeatDrawable(repeatMode: Int) = binding.playerControls.trackRepeat.run {
            val index = repeatModes.indexOf(repeatMode)
            icon = drawables[index]
            (icon as Animatable).start()
        }
        binding.playerControls.trackRepeat.setOnClickListener {
            val mode = when (viewModel.repeat.value) {
                REPEAT_MODE_OFF -> REPEAT_MODE_ALL
                REPEAT_MODE_ALL -> REPEAT_MODE_ONE
                else -> REPEAT_MODE_OFF
            }
            changeRepeatDrawable(mode)
            viewModel.onRepeat(mode)
        }

        observe(viewModel.repeat) {
            viewModel.repeatEnabled = false
            changeRepeatDrawable(it)
            viewModel.repeatEnabled = true
        }

        item ?: return
        val extensionClient = viewModel.extensionListFlow.getClient(item.clientId)
        binding.playerControls.trackHeart.run {
            if (extensionClient is LibraryClient) {
                isChecked = item.liked
                val likeListener = CheckBoxListener {
                    viewModel.likeTrack(item, it)
                }
                addOnCheckedStateChangedListener(likeListener)
                observe(item.onLiked) {
                    likeListener.enabled = false
                    isChecked = it
                    likeListener.enabled = true
                }
                isVisible = true
            } else isVisible = false
        }
    }

    private fun ItemPlayerTrackBinding.applyTrackDetails(
        client: String?,
        track: Track?,
        oldTrack: Track? = null
    ) {
        track?.cover.loadWith(expandedTrackCover, oldTrack?.cover) {
            collapsedContainer.collapsedTrackCover.load(it)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bgImage.setImageDrawable(it)
                bgImage.setRenderEffect(
                    RenderEffect.createBlurEffect(400f, 400.0f, Shader.TileMode.MIRROR)
                )
            } else bgImage.load(it, 16)
        }

        collapsedContainer.run {
            collapsedTrackArtist.text = track?.artists?.joinToString(", ") { it.name }
            collapsedTrackTitle.text = track?.title
            collapsedTrackTitle.isSelected = true
            collapsedTrackTitle.setHorizontallyScrolling(true)
        }

        playerControls.run {
            trackTitle.text = track?.title
            trackTitle.isSelected = true
            trackTitle.setHorizontallyScrolling(true)
            val artists = track?.artists
            val artistNames = artists?.joinToString(", ") { it.name } ?: ""
            val spannableString = SpannableString(artistNames)

            artists?.forEach { artist ->
                val start = artistNames.indexOf(artist.name)
                val end = start + artist.name.length
                val clickableSpan = PlayerItemSpan(
                    root.context, client, artist.toMediaItem(), onItemClicked
                )
                spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            trackArtist.text = spannableString
            trackArtist.movementMethod = LinkMovementMethod.getInstance()
        }

        expandedToolbar.run {
            val album = track?.album?.title
            title = if (album != null) context.getString(R.string.playing_from) else null
            subtitle = album
        }
    }

    private fun Context.getPlayerColors(bitmap: Bitmap?): PlayerColors {
        val defaultColors = defaultPlayerColors()
        bitmap ?: return defaultColors
        val preferences =
            applicationContext.getSharedPreferences(packageName, Context.MODE_PRIVATE)
        val dynamicPlayer = preferences.getBoolean(LookFragment.DYNAMIC_PLAYER, true)
        val imageColors =
            if (dynamicPlayer) getColorsFrom(bitmap) else defaultColors
        return imageColors ?: defaultColors
    }

    private fun ItemPlayerCollapsedBinding.applyColors(colors: PlayerColors) {
        root.setBackgroundColor(colors.background)
        collapsedProgressBar.setIndicatorColor(colors.accent)
        collapsedSeekBar.setIndicatorColor(colors.accent)
        collapsedBuffer.setIndicatorColor(colors.accent)
        collapsedBuffer.trackColor = colors.text
        collapsedTrackArtist.setTextColor(colors.text)
        collapsedTrackTitle.setTextColor(colors.text)
    }

    private fun ItemPlayerControlsBinding.applyColors(colors: PlayerColors) {
        val clickableState = ColorStateList.valueOf(colors.accent)
        seekBar.trackActiveTintList = clickableState
        seekBar.thumbTintList = clickableState
        progressBar.setIndicatorColor(colors.accent)
        bufferBar.setIndicatorColor(colors.accent)
        bufferBar.trackColor = colors.text
        trackCurrentTime.setTextColor(colors.text)
        trackTotalTime.setTextColor(colors.text)
        trackTitle.setTextColor(colors.text)
        trackArtist.setTextColor(colors.text)
    }

}
