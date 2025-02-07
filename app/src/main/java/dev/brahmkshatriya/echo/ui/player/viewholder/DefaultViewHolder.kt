package dev.brahmkshatriya.echo.ui.player.viewholder

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedVectorDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemPlayerControlsBinding
import dev.brahmkshatriya.echo.databinding.ItemPlayerSmallBinding
import dev.brahmkshatriya.echo.databinding.ItemPlayerTrackBinding
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.playback.MediaItemUtils.background
import dev.brahmkshatriya.echo.playback.MediaItemUtils.context
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.isLiked
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.ui.player.CheckBoxListener
import dev.brahmkshatriya.echo.ui.player.PlayerColors
import dev.brahmkshatriya.echo.ui.player.PlayerColors.Companion.defaultPlayerColors
import dev.brahmkshatriya.echo.ui.player.PlayerColors.Companion.getColorsFrom
import dev.brahmkshatriya.echo.ui.player.PlayerTrackAdapter.Listener
import dev.brahmkshatriya.echo.ui.player.PlayerTrackAdapter.ViewHolder
import dev.brahmkshatriya.echo.ui.settings.LookFragment
import dev.brahmkshatriya.echo.utils.image.load
import dev.brahmkshatriya.echo.utils.image.loadBitmap
import dev.brahmkshatriya.echo.utils.image.loadBlurred
import dev.brahmkshatriya.echo.utils.image.loadWithThumb
import dev.brahmkshatriya.echo.utils.ui.PlayerItemSpan
import dev.brahmkshatriya.echo.utils.ui.animateVisibility
import dev.brahmkshatriya.echo.utils.ui.dpToPx
import dev.brahmkshatriya.echo.utils.ui.toTimeString
import dev.brahmkshatriya.echo.viewmodels.UiViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.math.max
import kotlin.math.min

@OptIn(UnstableApi::class)
class DefaultViewHolder(
    private val binding: ItemPlayerTrackBinding,
    private val listener: Listener,
    private val cache: SimpleCache,
    private val settings: SharedPreferences,
    private val isTrackLikeClient: (String, (Boolean) -> Unit) -> Unit,
    private val players: MutableList<Player>
) : ViewHolder(binding.root) {

    override fun bind(item: MediaItem) {
        val extensionId = item.extensionId

        binding.applyTrackDetails(extensionId, item)
        this.item = item

        item.track.cover.loadBitmap(binding.root) { bitmap ->
            val colors = binding.root.context.getPlayerColors(bitmap)
            binding.bgGradient.imageTintList = ColorStateList.valueOf(colors.background)
            binding.expandedToolbar.run {
                setTitleTextColor(colors.text)
                setSubtitleTextColor(colors.text)
            }
            binding.collapsedContainer.applyColors(colors)
            binding.playerControls.applyColors(colors)

            if (showBackground()) binding.bgImage.loadBlurred(bitmap, 12f)
            else binding.bgImage.setImageDrawable(null)
        }

        binding.collapsedContainer.root.setOnClickListener {
            listener.onChangePlayerState(STATE_EXPANDED)
        }
        binding.expandedToolbar.setNavigationOnClickListener {
            listener.onChangePlayerState(STATE_COLLAPSED)
        }
        binding.collapsedContainer.playerClose.setOnClickListener {
            listener.onChangePlayerState(STATE_HIDDEN)
        }

        binding.bgPanel.start.handleGestures(true) { listener.onShowBackground(false) }
        binding.bgPanel.end.handleGestures(false) { listener.onShowBackground(false) }
        fun show() {
            val bgImage = binding.bgImage.drawable != null
            if (bgImage || binding.bgVideo.player.hasVideo() || item.background != null)
                listener.onShowBackground(true)
        }
        binding.trackPanel.start.handleGestures(true, ::show)
        binding.trackPanel.end.handleGestures(false, ::show)

        binding.playerControls.bind(item)
    }

    private fun View.handleGestures(start: Boolean, show: () -> Unit) {
        val listener = object : GestureDetector.SimpleOnGestureListener() {
            private var timer: Timer? = null
            private var seeking = false
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (playerSheetState != STATE_EXPANDED) return false
                if (!seeking) show()
                else onDoubleTap(e)
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (playerSheetState != STATE_EXPANDED) return false
                isPressed = true
                isPressed = false
                val amount = 10000L
                listener.onSeekTo(max(0, pos + if (start) -amount else amount))

                seeking = true
                timer?.cancel()
                val timer = Timer()
                this.timer = timer
                timer.schedule(1000) {
                    seeking = false
                }
                return true
            }
        }
        val detector = GestureDetector(context, listener)
        setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            performClick()
            true
        }
    }

    private var isCurrent: Boolean = false
    override fun onCurrentChanged(current: Current?) {
        isCurrent = current?.index == bindingAdapterPosition
        applyVideo()
    }

    private var backgroundPlayer: Player? = null
    private fun applyVideoVisibility(visible: Boolean) {
        binding.bgVideo.isVisible = visible
        binding.bgImage.isVisible = !visible
    }

    private var actualPlayer: Player? = null
    override fun onPlayer(player: Player?) {
        actualPlayer = player
        player?.addListener(playerListener)
        applyVideo()
    }

    private var item: MediaItem? = null
    private var oldBg: Streamable.Media.Background? = null
    private fun applyPlayerVideo() {
        if (oldBg != null && showBackground()) return
        if (isCurrent) {
            binding.bgVideo.resizeMode = RESIZE_MODE_FIT
            binding.bgVideo.player = actualPlayer
            val hasVideo = actualPlayer.hasVideo()
            applyVideoVisibility(hasVideo)
        } else {
            binding.bgVideo.player = null
            applyVideoVisibility(false)
        }
    }

    private fun applyVideo() {
        val background = item?.background
        if (background == null || background == oldBg || !showBackground())
            return applyPlayerVideo()

        cleanUp()
        oldBg = background
        actualPlayer?.addListener(playerListener)
        binding.bgVideo.run {
            resizeMode = RESIZE_MODE_ZOOM
            val bP = getPlayer(context, cache, background)
            players.add(bP)
            backgroundPlayer = bP
            player = backgroundPlayer
            applyVideoVisibility(true)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onTracksChanged(tracks: Tracks) {
            applyPlayerVideo()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            backgroundPlayer?.playWhenReady = isPlaying
        }
    }

    override fun cleanUp() {
        actualPlayer?.removeListener(playerListener)
        binding.bgVideo.player = null
        backgroundPlayer?.release()
        players.remove(backgroundPlayer)
        backgroundPlayer = null
        oldBg = null
    }

    private var offset = 0f
    override fun onPlayerOffsetChanged(it: Float) {
        val offset = max(0f, it)
        val height = binding.collapsedContainer.root.height
        binding.collapsedContainer.root.run {
            translationY = -height * offset
            alpha = 1 - offset
        }
        binding.expandedTrackCoverContainer.alpha = offset
        binding.expandedTrackCoverContainer.isVisible = offset != 0f
        this.offset = offset
    }

    private var playerSheetState = STATE_COLLAPSED
    override fun onPlayerStateChanged(state: Int) {
        playerSheetState = state
    }

    private var playerBgVisible = false
    override fun onPlayerBgVisibleChanged(visible: Boolean) {
        val animate = isCurrent
        if (!visible) {
            binding.expandedContainer.alpha = 1f
            binding.expandedContainer.isVisible = true
        }
        if (playerSheetState == STATE_EXPANDED) {
            binding.bgGradient.animateVisibility(!visible, animate)
            binding.expandedTrackCoverContainer.animateVisibility(!visible, animate)
        } else {
            binding.bgGradient.animateVisibility(true, animate)
            binding.expandedTrackCoverContainer.animateVisibility(offset != 0f, animate)
        }
        binding.expandedContainer.animateVisibility(!visible, false)
    }

    override fun onInfoSheetOffsetChanged(offset: Float) {
        if (playerBgVisible) {
            binding.bgGradient.isVisible = offset != 0f
            binding.bgGradient.alpha = offset
        } else {
            binding.expandedContainer.alpha = 1 - offset
            binding.expandedContainer.isVisible = offset != 1f
        }
    }

    override fun onSystemInsetsChanged(insets: UiViewModel.Insets) {
        binding.expandedTrackCoverContainer.applyInsets(insets, 24)
        binding.collapsedContainer.root.updatePaddingRelative(
            start = insets.start,
            end = insets.end
        )
    }

    override fun onCombinedInsetsChanged(insets: UiViewModel.Insets) {
        binding.collapsedContainer.run{
            val padding = 8.dpToPx(root.context)
            listOf(collapsedPlayerInfo, collapsedSeekBar, collapsedBuffer).forEach {
                it.updateLayoutParams<MarginLayoutParams> {
                    marginStart = insets.start + padding
                    marginEnd = insets.end + padding
                }
            }
        }
    }

    private fun Player?.hasVideo() =
        this?.currentTracks?.groups.orEmpty().any { it.type == C.TRACK_TYPE_VIDEO }

    private val likeListener =
        CheckBoxListener { item?.let { it1 -> listener.onItemLiked(it1, it) } }

    private fun ItemPlayerControlsBinding.bind(item: MediaItem) {
        isTrackLikeClient(item.extensionId) { trackHeart.isVisible = it }
        trackHeart.addOnCheckedStateChangedListener(likeListener)
        likeListener.enabled = false
        trackHeart.isChecked = item.isLiked
        likeListener.enabled = true

        seekBar.apply {
            addOnChangeListener { _, value, fromUser ->
                if (fromUser)
                    trackCurrentTime.text = value.toLong().toTimeString()
            }
            addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) = Unit
                override fun onStopTrackingTouch(slider: Slider) =
                    listener.onSeekTo(slider.value.toLong())
            })
        }

        binding.collapsedContainer.collapsedTrackPlayPause
            .addOnCheckedStateChangedListener(playPauseListener)
        trackPlayPause
            .addOnCheckedStateChangedListener(playPauseListener)

        trackNext.setOnClickListener {
            listener.onNext()
            it as MaterialButton
            (it.icon as Animatable).start()
        }
        trackPrevious.setOnClickListener {
            listener.onPrevious()
            it as MaterialButton
            (it.icon as Animatable).start()
        }
        trackShuffle.addOnCheckedStateChangedListener(shuffleListener)
        trackRepeat.setOnClickListener {
            val mode = when (currRepeatMode) {
                REPEAT_MODE_OFF -> REPEAT_MODE_ALL
                REPEAT_MODE_ALL -> REPEAT_MODE_ONE
                else -> REPEAT_MODE_OFF
            }
            listener.onRepeat(mode)
            changeRepeatDrawable(mode)
        }
    }

    var pos = 0
    override fun onProgressChanged(current: Int, buffered: Int) {
        val curr = max(0, if (isCurrent) current else 0)
        val buff = max(0, if (isCurrent) buffered else 0)
        pos = curr
        binding.collapsedContainer.run {
            collapsedBuffer.progress = buff
            collapsedSeekBar.progress = curr
        }
        binding.playerControls.run {
            if (!seekBar.isPressed) {
                bufferBar.progress = buff
                seekBar.value = min(curr.toFloat(), seekBar.valueTo)
                trackCurrentTime.text = curr.toLong().toTimeString()
            }
        }
    }

    override fun onBufferingChanged(buffering: Boolean) {
        binding.collapsedContainer.collapsedProgressBar.isVisible = buffering
        binding.playerControls.progressBar.isVisible = buffering
    }

    override fun onTotalDurationChanged(totalDuration: Int?) {
        val duration = (if (isCurrent) totalDuration else item?.track?.duration?.toInt()) ?: 0
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

    private val playPauseListener = CheckBoxListener { listener.onPlayPause(it) }
    override fun onPlayPause(play: Boolean) {
        playPauseListener.enabled = false
        binding.playerControls.trackPlayPause.isChecked = play
        binding.collapsedContainer.collapsedTrackPlayPause.isChecked = play
        playPauseListener.enabled = true
    }

    override fun onNextEnabled(enabled: Boolean) {
        binding.playerControls.trackNext.isEnabled = enabled
    }

    override fun onPreviousEnabled(enabled: Boolean) {
        binding.playerControls.trackPrevious.isEnabled = enabled
    }

    private val shuffleListener = CheckBoxListener { listener.onShuffle(it) }
    override fun onShuffleChanged(shuffled: Boolean) {
        shuffleListener.enabled = false
        binding.playerControls.trackShuffle.isChecked = shuffled
        shuffleListener.enabled = true
    }

    private val trackRepeat get() = binding.playerControls.trackRepeat
    private var currRepeatMode = 0
    private val animatedVectorDrawables = trackRepeat.context.run {
        fun asAnimated(id: Int) =
            AppCompatResources.getDrawable(this, id) as AnimatedVectorDrawable
        listOf(
            asAnimated(R.drawable.ic_repeat_one_to_repeat_off_40dp),
            asAnimated(R.drawable.ic_repeat_off_to_repeat_40dp),
            asAnimated(R.drawable.ic_repeat_to_repeat_one_40dp)
        )
    }
    private val drawables = trackRepeat.context.run {
        fun asDrawable(id: Int) = AppCompatResources.getDrawable(this, id)!!
        listOf(
            asDrawable(R.drawable.ic_repeat_off_40dp),
            asDrawable(R.drawable.ic_repeat_40dp),
            asDrawable(R.drawable.ic_repeat_one_40dp),
        )
    }
    private val repeatModes = listOf(REPEAT_MODE_OFF, REPEAT_MODE_ALL, REPEAT_MODE_ONE)
    private fun changeRepeatDrawable(repeatMode: Int) = trackRepeat.run {
        if (currRepeatMode == repeatMode) {
            icon = drawables[repeatModes.indexOf(repeatMode)]
            return
        }
        val index = repeatModes.indexOf(repeatMode)
        icon = animatedVectorDrawables[index]
        (icon as Animatable).start()
    }

    override fun onRepeatModeChanged(mode: Int) {
        changeRepeatDrawable(mode)
        currRepeatMode = mode
    }

    private fun ItemPlayerTrackBinding.applyTrackDetails(
        client: String,
        item: MediaItem,
    ) {
        val track = item.track
        track.cover.loadWithThumb(
            expandedTrackCover,
            this@DefaultViewHolder.item?.track?.cover,
            R.drawable.art_music
        ) {
            collapsedContainer.collapsedTrackCover.load(it)
        }

        collapsedContainer.run {
            collapsedTrackArtist.text = item.track.toMediaItem().subtitleWithE
            collapsedTrackArtist.isSelected = true
            collapsedTrackArtist.setHorizontallyScrolling(true)
            collapsedTrackTitle.text = track.title
            collapsedTrackTitle.isSelected = true
            collapsedTrackTitle.setHorizontallyScrolling(true)
        }

        playerControls.run {
            applyTitles(track, trackTitle, trackArtist, client, listener)
        }

        expandedToolbar.run {
            val itemContext = item.context
            title = if (itemContext != null) context.getString(R.string.playing_from) else null
            subtitle = itemContext?.title

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_more -> {
                        listener.onMoreClicked(item)
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
        val artistNames = track.toMediaItem().subtitleWithE ?: ""
        val spannableString = SpannableString(artistNames)

        artists.forEach { artist ->
            val start = artistNames.indexOf(artist.name)
            val end = start + artist.name.length
            val clickableSpan = PlayerItemSpan(
                trackArtist.context, client to artist.toMediaItem(), listener::onItemClicked
            )
            spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        trackArtist.text = spannableString
        trackArtist.isSelected = true
        trackArtist.setHorizontallyScrolling(true)
        trackArtist.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun showBackground() =
        settings.getBoolean(LookFragment.SHOW_BACKGROUND, true)

    private fun isDynamic() =
        settings.getBoolean(LookFragment.DYNAMIC_PLAYER, true)

    private fun Context.getPlayerColors(bitmap: Bitmap?): PlayerColors {
        val defaultColors = defaultPlayerColors()
        bitmap ?: return defaultColors
        val imageColors = if (isDynamic()) getColorsFrom(bitmap) else defaultColors
        return imageColors ?: defaultColors
    }

    private fun ItemPlayerSmallBinding.applyColors(colors: PlayerColors) {
        collapsedBg.setBackgroundColor(colors.background)
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

    @OptIn(UnstableApi::class)
    fun getPlayer(
        context: Context, cache: SimpleCache, video: Streamable.Media.Background
    ): ExoPlayer {
        val cacheFactory = CacheDataSource
            .Factory().setCache(cache)
            .setUpstreamDataSourceFactory(
                DefaultHttpDataSource.Factory()
                    .setDefaultRequestProperties(video.request.headers)
            )
        val factory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(cacheFactory)
        val player = ExoPlayer.Builder(context).setMediaSourceFactory(factory).build()
        player.setMediaItem(MediaItem.fromUri(video.request.url.toUri()))
        player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
        player.volume = 0f
        player.prepare()
        player.play()
        return player
    }
}