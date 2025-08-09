package dev.brahmkshatriya.echo.ui.feed.viewholders

import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Radio
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemShelfMediaBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.PlayerState.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.ui.feed.FeedClickListener
import dev.brahmkshatriya.echo.ui.feed.FeedType
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadInto

class MediaViewHolder(
    parent: ViewGroup,
    listener: FeedClickListener,
    getAllTracks: (FeedType) -> Pair<List<Track>, Int>,
    private val binding: ItemShelfMediaBinding = ItemShelfMediaBinding.inflate(
        LayoutInflater.from(parent.context), parent, false
    )
) : FeedViewHolder<FeedType.Media>(binding.root) {
    var feed: FeedType.Media? = null

    init {
        binding.coverContainer.cover.clipToOutline = true
        binding.root.setOnClickListener {
            when (val item = feed?.item) {
                is Track -> {
                    val (tracks, pos) = getAllTracks(feed!!)
                    listener.onTracksClicked(it, feed?.extensionId, feed?.context, tracks, pos)
                }

                else -> listener.onMediaClicked(it, feed?.extensionId, item, feed?.context)
            }
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
        binding.play.setOnClickListener {
            listener.onPlayClicked(
                it, feed?.extensionId, feed?.item, null, false
            )
        }
    }

    override fun bind(feed: FeedType.Media) {
        this.feed = feed
        binding.bind(feed.item, feed.number?.toInt())
    }

    override fun canBeSwiped() = feed?.item is Track
    override fun onSwipe() = feed

    companion object {
        fun ItemShelfMediaBinding.bind(item: EchoMediaItem, index: Int? = null) {
            title.text = if (index == null) item.title
            else root.context.getString(R.string.n_dot_x, index + 1, item.title)
            val subtitleText = item.subtitleWithE
            subtitle.text = subtitleText
            subtitle.isVisible = !subtitleText.isNullOrBlank()
            coverContainer.run {
                applyCover(item, cover, listBg1, listBg2, icon)
                isPlaying.setBackgroundResource(
                    if (item is Artist) R.drawable.rounded_rectangle_cover_profile
                    else R.drawable.rounded_rectangle_cover
                )
            }
            play.isVisible = item !is Track
        }

        val EchoMediaItem.placeHolder
            get() = when (this) {
                is Track -> R.drawable.art_music
                is Artist -> R.drawable.art_artist
                is Album -> R.drawable.art_album
                is Playlist -> R.drawable.art_library_music
                is Radio -> R.drawable.art_sensors
            }

        val EchoMediaItem.icon
            get() = when (this) {
                is Track -> R.drawable.ic_music
                is Artist -> R.drawable.ic_artist
                is Album -> R.drawable.ic_album
                is Playlist -> R.drawable.ic_library_music
                is Radio -> R.drawable.ic_sensors
            }

        fun applyCover(
            item: EchoMediaItem,
            cover: ImageView,
            listBg1: View,
            listBg2: View,
            icon: ImageView,
        ) {
            icon.isVisible = when (item) {
                is Track, is Artist, is Album -> false
                else -> true
            }
            icon.setImageResource(item.icon)
            cover.setBackgroundResource(
                if (item is Artist) R.drawable.rounded_rectangle_cover_profile
                else R.drawable.rounded_rectangle_cover
            )
            val bgVisible = item is EchoMediaItem.Lists
            listBg1.isVisible = bgVisible
            listBg2.isVisible = bgVisible
            item.cover.loadInto(cover, item.placeHolder)
        }
    }

    override fun onCurrentChanged(current: PlayerState.Current?) {
        val isPlaying = current.isPlaying(feed?.item?.id)
        binding.coverContainer.isPlaying.isVisible = isPlaying
        (binding.coverContainer.isPlaying.icon as Animatable).start()
    }
}