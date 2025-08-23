package dev.brahmkshatriya.echo.ui.feed.viewholders

import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.databinding.ItemShelfMediaGridBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.PlayerState.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.ui.feed.FeedClickListener
import dev.brahmkshatriya.echo.ui.feed.FeedType
import dev.brahmkshatriya.echo.ui.feed.viewholders.MediaViewHolder.Companion.applyCover
import dev.brahmkshatriya.echo.ui.feed.viewholders.MediaViewHolder.Companion.subtitle

class MediaGridViewHolder(
    parent: ViewGroup,
    listener: FeedClickListener,
    getAllTracks: (FeedType) -> Pair<List<Track>, Int>,
    private val binding: ItemShelfMediaGridBinding = ItemShelfMediaGridBinding.inflate(
        LayoutInflater.from(parent.context), parent, false
    )
) : FeedViewHolder<FeedType.MediaGrid>(binding.root) {
    private var feed: FeedType.MediaGrid? = null

    init {
        binding.coverContainer.cover.clipToOutline = true
        binding.root.setOnClickListener {
            when (val item = feed?.item) {
                is Track -> {
                    if (item.isPlayable != Track.Playable.Yes) {
                        listener.onMediaClicked(it, feed?.extensionId, item, feed?.context)
                        return@setOnClickListener
                    }
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
    }

    override fun bind(feed: FeedType.MediaGrid) = with(binding) {
        this@MediaGridViewHolder.feed = feed
        val item = feed.item
        val index = feed.number
        title.text = if (index == null) item.title
        else root.context.getString(R.string.n_dot_x, index + 1, item.title)
        val subtitleText = item.subtitle(root.context)
        subtitle.text = subtitleText
        subtitle.isVisible = !subtitleText.isNullOrBlank()
        coverContainer.run { applyCover(item, cover, listBg1, listBg2, icon) }
    }


    override fun onCurrentChanged(current: PlayerState.Current?) {
        val isPlaying = current.isPlaying(feed?.item?.id)
        binding.coverContainer.isPlaying.isVisible = isPlaying
        (binding.coverContainer.isPlaying.icon as Animatable).start()
    }
}