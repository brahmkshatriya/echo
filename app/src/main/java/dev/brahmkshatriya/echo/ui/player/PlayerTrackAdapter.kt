package dev.brahmkshatriya.echo.ui.player

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedVectorDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.TrackLikeClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemPlayerCollapsedBinding
import dev.brahmkshatriya.echo.databinding.ItemPlayerControlsBinding
import dev.brahmkshatriya.echo.databinding.ItemPlayerTrackBinding
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.extensions.isClient
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.context
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLiked
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLoaded
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.MediaItemUtils.video
import dev.brahmkshatriya.echo.playback.source.MediaResolver
import dev.brahmkshatriya.echo.ui.adapter.LifeCycleListAdapter
import dev.brahmkshatriya.echo.ui.player.PlayerColors.Companion.defaultPlayerColors
import dev.brahmkshatriya.echo.ui.player.PlayerColors.Companion.getColorsFrom
import dev.brahmkshatriya.echo.ui.settings.LookFragment
import dev.brahmkshatriya.echo.utils.animateVisibility
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.emit
import dev.brahmkshatriya.echo.utils.load
import dev.brahmkshatriya.echo.utils.loadBitmap
import dev.brahmkshatriya.echo.utils.loadWith
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.toTimeString
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.isLandscape
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class PlayerTrackAdapter(
    val fragment: Fragment,
    private val listener: Listener
) : LifeCycleListAdapter<MediaItem, PlayerTrackAdapter.ViewHolder>(DiffCallback) {

    interface Listener {
        fun onMoreClicked(clientId: String?, item: EchoMediaItem, loaded: Boolean)
        fun onItemClicked(clientId: String?, item: EchoMediaItem)
    }

    object DiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
            oldItem.mediaId == newItem.mediaId

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem) =
            oldItem == newItem

    }

    override fun createHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemPlayerTrackBinding.inflate(inflater, parent, false))
    }

    inner class ViewHolder(val binding: ItemPlayerTrackBinding) : Holder<MediaItem>(binding.root) {
        override fun bind(item: MediaItem) {
            onBind(bindingAdapterPosition)
        }
    }

    private val viewModel by fragment.activityViewModels<PlayerViewModel>()
    private val uiViewModel by fragment.activityViewModels<UiViewModel>()

    @OptIn(UnstableApi::class)
    fun ViewHolder.onBind(position: Int) {
        binding.bgVisualizer.processor = viewModel.fftAudioProcessor

        val item = getItem(position) ?: return
        val clientId = item.clientId

        binding.applyTrackDetails(clientId, item)

        lifecycleScope.launch {
            val bitmap = item.track.cover?.loadBitmap(binding.root.context)
            val colors = binding.root.context.getPlayerColors(bitmap)
            binding.root.setBackgroundColor(colors.background)
            binding.bgInfoSpace.setBackgroundColor(colors.background)
            binding.bgVisualizer.setColors(colors.accent)
            binding.bgGradient.imageTintList = ColorStateList.valueOf(colors.background)
            binding.bgInfoGradient.imageTintList = ColorStateList.valueOf(colors.background)
            binding.expandedToolbar.run {
                setTitleTextColor(colors.text)
                setSubtitleTextColor(colors.text)
            }
            binding.collapsedContainer.applyColors(colors)
            binding.playerControls.applyColors(colors)

            binding.bgInfoTitle.setTextColor(colors.text)
            binding.bgInfoArtist.setTextColor(colors.text)
            runCatching {
                Glide.with(binding.bgImage).load(bitmap)
                    .apply(RequestOptions.bitmapTransform(BlurTransformation(2, 4)))
                    .into(binding.bgImage)
            }
        }

        binding.collapsedContainer.root.setOnClickListener {
            emit(uiViewModel.changePlayerState) { STATE_EXPANDED }
        }
        binding.expandedToolbar.setNavigationOnClickListener {
            emit(uiViewModel.changePlayerState) { STATE_COLLAPSED }
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
            binding.expandedTrackCoverContainer.alpha = offset
        }

        observe(uiViewModel.playerBgVisibleState) {
            val animate = viewModel.currentFlow.value?.index == bindingAdapterPosition
            binding.bgInfoContainer.animateVisibility(it, animate)
            if (uiViewModel.playerSheetState.value == STATE_EXPANDED) {
                binding.bgGradient.animateVisibility(!it, animate)
                binding.expandedTrackCoverContainer.animateVisibility(!it, animate)
            } else {
                binding.bgGradient.animateVisibility(true, animate)
                binding.expandedTrackCoverContainer.isVisible = true
            }
            if (!it) {
                binding.expandedContainer.alpha = 1f
                binding.expandedContainer.isVisible = true
            }
        }

        binding.bgImage.setOnClickListener {
            emit(uiViewModel.playerBgVisibleState) { false }
            if (uiViewModel.infoSheetState.value == STATE_EXPANDED)
                emit(uiViewModel.changeInfoState) { STATE_COLLAPSED }
        }

//        binding.bgVideo.setOnClickListener {
//            emit(uiViewModel.playerBgVisibleState) { false }
//            if (uiViewModel.infoSheetState.value == STATE_EXPANDED)
//                emit(uiViewModel.changeInfoState) { STATE_COLLAPSED }
//        }

        binding.expandedTrackCover.setOnClickListener {
            emit(uiViewModel.playerBgVisibleState) { true }
        }

        observe(uiViewModel.infoSheetOffset) {
            if (uiViewModel.playerBgVisibleState.value) {
                binding.bgGradient.isVisible = it != 0f
                binding.bgGradient.alpha = it
                if (!binding.root.context.isLandscape())
                    binding.bgInfoContainer.alpha = 1 - it
            } else {
                binding.expandedContainer.alpha = 1 - it
                binding.expandedContainer.isVisible = it != 1f
            }
        }

        observe(uiViewModel.systemInsets) {
            binding.expandedTrackCoverContainer.applyInsets(it, 24)
            binding.bgInfoContainer.applyInsets(it)
            binding.bgInfoSpace.updateLayoutParams<MarginLayoutParams> {
                val context = binding.bgInfoSpace.context
                height = (if (context.isLandscape()) 0 else 64).dpToPx(context) + it.bottom
                bottomMargin = -it.bottom
            }
            binding.collapsedContainer.root.updatePaddingRelative(start = it.start, end = it.end)
        }

        binding.playerControls.bind(this, item, clientId)

        //VIDEO STUFF
        binding.bgVideo.apply {
            val video = item.video
            isVisible = video != null
            if (video != null) {
                if (video is Streamable.Media.WithVideo.Only && video.looping) {
                    val player = MediaResolver.getPlayer(context, viewModel.cache, video)
                    setPlayer(player)
                } else {
                    observe(viewModel.currentFlow) {
                        val isCurrent = it?.index == bindingAdapterPosition
                        if (!isCurrent) return@observe
                        post {
                            setPlayer(null)
                            setPlayer(viewModel.browser.value)
                        }
                    }
                }
                resizeMode = if (video.crop) RESIZE_MODE_ZOOM else RESIZE_MODE_FIT
            }
        }
    }

    private fun ItemPlayerControlsBinding.bind(
        viewHolder: ViewHolder,
        item: MediaItem,
        clientId: String
    ) {

        fun <T> observe(flow: Flow<T>, block: (T) -> Unit) {
            viewHolder.observe(flow) { block(it) }
        }
        fun <T> observeCurrent(flow: Flow<T>, block: (T?) -> Unit) = with(viewHolder) {
            observe(flow) {
                if (viewModel.currentFlow.value?.index == bindingAdapterPosition) block(it)
                else block(null)
            }
        }

        trackHeart.run {
            val extension = viewModel.extensionListFlow.getExtension(clientId) ?: return
            val isTrackLikeClient = extension.isClient<TrackLikeClient>()
            isVisible = isTrackLikeClient

            val likeListener = viewModel.likeListener
            if (isTrackLikeClient) {
                addOnCheckedStateChangedListener(likeListener)
                observeCurrent(viewModel.isLiked) {
                    it ?: return@observeCurrent
                    likeListener.enabled = false
                    trackHeart.isChecked = it
                    likeListener.enabled = true
                }
            } else removeOnCheckedStateChangedListener(likeListener)
            viewModel.isLiked.value = item.isLiked
        }

        seekBar.apply {
            addOnChangeListener { _, value, fromUser ->
                if (fromUser)
                    trackCurrentTime.text = value.toLong().toTimeString()
            }
            addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) = Unit
                override fun onStopTrackingTouch(slider: Slider) =
                    viewModel.seekTo(slider.value.toLong())
            })
        }

        val collapsedContainer = viewHolder.binding.collapsedContainer
        observeCurrent(viewModel.progress) {
            val (current, buffered) = it ?: (0 to 0)
            collapsedContainer.run {
                collapsedBuffer.progress = buffered
                collapsedSeekBar.progress = current
            }
            if (!seekBar.isPressed) {
                bufferBar.progress = buffered
                seekBar.value = min(current.toFloat(), seekBar.valueTo)
                trackCurrentTime.text = current.toLong().toTimeString()
            }
        }

        viewHolder.observe(viewModel.buffering) { buffering ->
            collapsedContainer.run {
                collapsedProgressBar.isVisible = buffering
                collapsedTrackPlayPause.isEnabled = !buffering
            }
            progressBar.isVisible = buffering
            seekBar.isEnabled = !buffering
            trackPlayPause.isEnabled = !buffering

        }

        observeCurrent(viewModel.totalDuration) {
            val duration = it ?: item.track.duration?.toInt() ?: return@observeCurrent
            collapsedContainer.run {
                collapsedSeekBar.max = duration
                collapsedBuffer.max = duration
            }
            bufferBar.max = duration
            seekBar.apply {
                value = min(value, duration.toFloat())
                valueTo = 1f + duration
            }
            trackTotalTime.text = duration.toLong().toTimeString()
        }


        val playPauseListener = viewModel.playPauseListener

        collapsedContainer.collapsedTrackPlayPause
            .addOnCheckedStateChangedListener(playPauseListener)
        trackPlayPause
            .addOnCheckedStateChangedListener(playPauseListener)

        observe(viewModel.isPlaying) {
            playPauseListener.enabled = false
            trackPlayPause.isChecked = it
            collapsedContainer.collapsedTrackPlayPause.isChecked = it
            playPauseListener.enabled = true
        }

        observe(viewModel.nextEnabled) {
            trackNext.isEnabled = it
        }
        trackNext.setOnClickListener {
            viewModel.seekToNext()
            it as MaterialButton
            (it.icon as Animatable).start()
        }
        observe(viewModel.previousEnabled) {
            trackPrevious.isEnabled = it
        }
        trackPrevious.setOnClickListener {
            viewModel.seekToPrevious()
            it as MaterialButton
            (it.icon as Animatable).start()
        }

        val shuffleListener = viewModel.shuffleListener
        trackShuffle.addOnCheckedStateChangedListener(shuffleListener)
        observe(viewModel.shuffleMode) {
            shuffleListener.enabled = false
            trackShuffle.isChecked = it
            shuffleListener.enabled = true
        }

        val animatedVectorDrawables = trackRepeat.context.run {
            fun asAnimated(id: Int) =
                AppCompatResources.getDrawable(this, id) as AnimatedVectorDrawable
            listOf(
                asAnimated(R.drawable.ic_repeat_one_to_repeat_off_40dp),
                asAnimated(R.drawable.ic_repeat_off_to_repeat_40dp),
                asAnimated(R.drawable.ic_repeat_to_repeat_one_40dp)
            )
        }
        val drawables = trackRepeat.context.run {
            fun asDrawable(id: Int) = AppCompatResources.getDrawable(this, id)!!
            listOf(
                asDrawable(R.drawable.ic_repeat_off_40dp),
                asDrawable(R.drawable.ic_repeat_40dp),
                asDrawable(R.drawable.ic_repeat_one_40dp),
            )
        }

        val repeatModes = listOf(REPEAT_MODE_OFF, REPEAT_MODE_ALL, REPEAT_MODE_ONE)
        var currRepeatMode = viewModel.repeatMode.value
        fun changeRepeatDrawable(repeatMode: Int) = trackRepeat.run {
            if (currRepeatMode == repeatMode) {
                icon = drawables[repeatModes.indexOf(repeatMode)]
                return
            }
            val index = repeatModes.indexOf(repeatMode)
            icon = animatedVectorDrawables[index]
            (icon as Animatable).start()
        }
        trackRepeat.setOnClickListener {
            val mode = when (viewModel.repeatMode.value) {
                REPEAT_MODE_OFF -> REPEAT_MODE_ALL
                REPEAT_MODE_ALL -> REPEAT_MODE_ONE
                else -> REPEAT_MODE_OFF
            }
            viewModel.onRepeat(mode)
            changeRepeatDrawable(mode)
        }

        observe(viewModel.repeatMode) {
            viewModel.repeatEnabled = false
            changeRepeatDrawable(it)
            currRepeatMode = it
            viewModel.repeatEnabled = true
        }
    }

    private fun ItemPlayerTrackBinding.applyTrackDetails(
        client: String,
        item: MediaItem,
    ) {
        val track = item.track
        track.cover.loadWith(expandedTrackCover) {
            collapsedContainer.collapsedTrackCover.load(it)
        }

        collapsedContainer.run {
            collapsedTrackArtist.text = track.artists.joinToString(", ") { it.name }
            collapsedTrackTitle.text = track.title
            collapsedTrackTitle.isSelected = true
            collapsedTrackTitle.setHorizontallyScrolling(true)
        }

        playerControls.run {
            applyTitles(track, trackTitle, trackArtist, client, listener)
        }
        applyTitles(track, bgInfoTitle, bgInfoArtist, client, listener)

        expandedToolbar.run {
            val itemContext = item.context
            title = if (itemContext != null) context.getString(R.string.playing_from) else null
            subtitle = itemContext?.title

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_more -> {
                        listener.onMoreClicked(client, track.toMediaItem(), item.isLoaded)
                        true
                    }

                    else -> false
                }
            }
        }
    }

    private fun applyTitles(
        track: Track, trackTitle: TextView, trackArtist: TextView,
        client: String, listener: Listener
    ) {
        trackTitle.text = track.title
        trackTitle.isSelected = true
        trackTitle.setHorizontallyScrolling(true)
        val artists = track.artists
        val artistNames = artists.joinToString(", ") { it.name }
        val spannableString = SpannableString(artistNames)

        artists.forEach { artist ->
            val start = artistNames.indexOf(artist.name)
            val end = start + artist.name.length
            val clickableSpan = PlayerItemSpan(
                trackArtist.context, client, artist.toMediaItem(), listener::onItemClicked
            )
            spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        trackArtist.text = spannableString
        trackArtist.movementMethod = LinkMovementMethod.getInstance()
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
//        root.setBackgroundColor(colors.background)
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
