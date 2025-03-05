package dev.brahmkshatriya.echo.ui.shelf.adapter

import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfCategoryBinding
import dev.brahmkshatriya.echo.databinding.ItemShelfTrackBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.PlayerState.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadInto
import dev.brahmkshatriya.echo.utils.ui.UiUtils.marquee

sealed class MediaItemShelfViewHolder(view: View) : ShelfAdapter.ViewHolder(view) {
    var item: Shelf.Item? = null
    override fun bind(item: Shelf?) {
        val shelfItem = (item as? Shelf.Item)
        this.item = shelfItem
        bind(shelfItem?.media, shelfItem?.loadTracks ?: false)
    }

    override fun onCurrentChanged(current: PlayerState.Current?) {
        onIsPlayingChanged(current.isPlaying(item?.media?.id))
    }

    abstract fun bind(mediaItem: EchoMediaItem?, loadTracks: Boolean)
    abstract fun onIsPlayingChanged(isPlaying: Boolean)

    class Track(
        val adapter: ShelfAdapter,
        val listener: ShelfAdapter.Listener,
        val binding: ItemShelfTrackBinding
    ) : MediaItemShelfViewHolder(binding.root) {

        constructor(
            adapter: ShelfAdapter,
            listener: ShelfAdapter.Listener,
            inflater: LayoutInflater,
            parent: ViewGroup
        ) : this(
            adapter,
            listener,
            ItemShelfTrackBinding.inflate(inflater, parent, false)
        )

        init {
            binding.root.setOnClickListener {
                val track = (item?.media as? EchoMediaItem.TrackItem)?.track
                val tracks = adapter.getTracks()
                val index = tracks.indexOf(track)
                listener.onTrackClicked(extensionId, tracks, index, null, it)
            }
            binding.root.setOnLongClickListener {
                val track = (item?.media as? EchoMediaItem.TrackItem)?.track
                val tracks = adapter.getTracks()
                val index = tracks.indexOf(track)
                listener.onTrackLongClicked(extensionId, tracks, index, null, it)
                true
            }
            binding.more.setOnClickListener {
                binding.root.performLongClick()
            }
        }

        override fun bind(mediaItem: EchoMediaItem?, loadTracks: Boolean) {
            mediaItem ?: return
            binding.title.text = mediaItem.title
            binding.subtitle.text = mediaItem.subtitleWithE
            binding.subtitle.isVisible = !mediaItem.subtitleWithE.isNullOrBlank()
            mediaItem.cover.loadInto(binding.cover, mediaItem.placeHolder)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            binding.isPlaying.isVisible = isPlaying
            (binding.isPlaying.drawable as Animatable).start()
        }
    }

    class List(view: View) : MediaItemShelfViewHolder(view) {
        override fun bind(mediaItem: EchoMediaItem?, loadTracks: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            TODO("Not yet implemented")
        }
    }

    class Profile(view: View) : MediaItemShelfViewHolder(view) {
        override fun bind(mediaItem: EchoMediaItem?, loadTracks: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            TODO("Not yet implemented")
        }
    }

    companion object {
        val EchoMediaItem.placeHolder
            get() = when (this) {
                is EchoMediaItem.TrackItem -> R.drawable.art_music
                is EchoMediaItem.Profile.ArtistItem -> R.drawable.art_artist
                is EchoMediaItem.Profile.UserItem -> R.drawable.art_person
                is EchoMediaItem.Lists.AlbumItem -> R.drawable.art_album
                is EchoMediaItem.Lists.PlaylistItem -> R.drawable.art_library_music
                is EchoMediaItem.Lists.RadioItem -> R.drawable.art_sensors
            }

        val EchoMediaItem.icon
            get() = when (this) {
                is EchoMediaItem.TrackItem -> R.drawable.ic_music
                is EchoMediaItem.Profile.ArtistItem -> R.drawable.ic_artist
                is EchoMediaItem.Profile.UserItem -> R.drawable.ic_person
                is EchoMediaItem.Lists.AlbumItem -> R.drawable.ic_album
                is EchoMediaItem.Lists.PlaylistItem -> R.drawable.ic_library_music
                is EchoMediaItem.Lists.RadioItem -> R.drawable.ic_sensors
            }

        fun getViewType(item: Shelf.Item): Int {
            return when (item.media) {
                is EchoMediaItem.TrackItem -> 1
                is EchoMediaItem.Lists -> 2
                is EchoMediaItem.Profile -> 3
            }
        }

        fun create(
            listener: ShelfAdapter.Listener,
            adapter: ShelfAdapter,
            inflater: LayoutInflater,
            parent: ViewGroup,
            viewType: Int
        ) = when (viewType) {
            1 -> Track(adapter, listener, inflater, parent)
//            2 -> List(view)
//            3 -> Profile(view)
//            else -> error("unknown view type")
            else -> Test(listener, inflater, parent)
        }
    }

    class Test(
        listener: ShelfAdapter.Listener,
        val binding: ItemShelfCategoryBinding
    ) : MediaItemShelfViewHolder(binding.root) {

        constructor(
            listener: ShelfAdapter.Listener,
            inflater: LayoutInflater,
            parent: ViewGroup
        ) : this(
            listener,
            ItemShelfCategoryBinding.inflate(inflater, parent, false)
        )

        init {
            binding.root.setOnClickListener {
                listener.onMediaItemClicked(extensionId, item?.media, it)
            }
            binding.root.setOnLongClickListener {
                listener.onMediaItemLongClicked(extensionId, item?.media, it)
                true
            }
            binding.title.marquee()
        }

        override fun bind(mediaItem: EchoMediaItem?, loadTracks: Boolean) {
            binding.title.text = mediaItem?.title
            binding.subtitle.text = mediaItem?.subtitle
            binding.subtitle.isVisible = !mediaItem?.subtitle.isNullOrBlank()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            binding.titleCard.isSelected = isPlaying
        }
    }
}