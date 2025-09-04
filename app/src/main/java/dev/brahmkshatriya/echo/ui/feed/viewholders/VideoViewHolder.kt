package dev.brahmkshatriya.echo.ui.feed.viewholders

import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemShelfVideoBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.PlayerState.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.ui.feed.FeedClickListener
import dev.brahmkshatriya.echo.ui.feed.FeedType
import dev.brahmkshatriya.echo.ui.feed.viewholders.MediaViewHolder.Companion.icon
import dev.brahmkshatriya.echo.ui.feed.viewholders.MediaViewHolder.Companion.placeHolder
import dev.brahmkshatriya.echo.ui.feed.viewholders.MediaViewHolder.Companion.subtitle
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadInto

class VideoViewHolder(
    parent: ViewGroup,
    listener: FeedClickListener,
    getAllTracks: (FeedType) -> Pair<List<Track>, Int>,
    private val binding: ItemShelfVideoBinding = ItemShelfVideoBinding.inflate(
        LayoutInflater.from(parent.context), parent, false
    )
) : FeedViewHolder<FeedType.Video>(binding.root) {

    var feed: FeedType.Video? = null

    init {
        binding.artistCover.clipToOutline = true
        binding.cover.clipToOutline = true
        binding.root.setOnClickListener {
            val track = feed?.item
            if (track?.isPlayable != Track.Playable.Yes) {
                listener.onMediaClicked(it, feed?.extensionId, track, feed?.context)
                return@setOnClickListener
            }
            val (tracks, pos) = getAllTracks(feed!!)
            listener.onTracksClicked(it, feed?.extensionId, feed?.context, tracks, pos)
        }
        binding.root.setOnLongClickListener {
            listener.onMediaLongClicked(
                it, feed?.extensionId, feed?.item,
                feed?.context, feed?.tabId, bindingAdapterPosition
            )
            true
        }
        binding.more.setOnClickListener {
            listener.onMediaLongClicked(
                it, feed?.extensionId, feed?.item,
                feed?.context, feed?.tabId, bindingAdapterPosition
            )
        }
    }

    override fun bind(feed: FeedType.Video) = with(binding) {
        this@VideoViewHolder.feed = feed
        val track = feed.item
        title.text = track.title
        val sub = track.subtitle(root.context)
        subtitle.text = sub
        subtitle.isVisible = !sub.isNullOrBlank()
        track.cover.loadInto(binding.cover, track.placeHolder)
        val artist = track.artists.firstOrNull()
        artist?.cover.loadInto(binding.artistCover, artist?.icon)
        artistCover.isVisible = artist != null
    }

    override fun onCurrentChanged(current: PlayerState.Current?) {
        val isPlaying = current.isPlaying(feed?.item?.id)
        binding.isPlaying.isVisible = isPlaying
        (binding.isPlaying.icon as Animatable).start()
    }
}