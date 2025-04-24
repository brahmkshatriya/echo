package dev.brahmkshatriya.echo.ui.shelf.adapter

import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfMediaBinding
import dev.brahmkshatriya.echo.databinding.ItemShelfTrackCardBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.PlayerState.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.ui.media.adapter.TrackAdapter
import dev.brahmkshatriya.echo.ui.media.adapter.TrackAdapter.Companion.subtitleWithDuration
import dev.brahmkshatriya.echo.ui.shelf.adapter.lists.MediaItemShelfListsViewHolder.Companion.applyCover
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadInto
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyTranslationYAnimation

sealed class MediaItemViewHolder(view: View) : ShelfAdapter.ViewHolder(view) {

    interface Listener : TrackAdapter.Listener {
        fun onMediaItemClicked(extensionId: String?, item: EchoMediaItem?, it: View?)
        fun onMediaItemLongClicked(extensionId: String?, item: EchoMediaItem?, it: View) {}
        fun onMediaItemPlayClicked(extensionId: String?, item: EchoMediaItem?, it: View) {}
    }

    var item: Shelf.Item? = null
    override fun bind(item: Shelf?) {
        val shelfItem = item as? Shelf.Item
        this.item = shelfItem
        bind(shelfItem?.media, shelfItem?.loadTracks ?: false)
    }

    override fun onCurrentChanged(current: PlayerState.Current?) {
        onIsPlayingChanged(current.isPlaying(item?.media?.id))
    }

    abstract fun bind(mediaItem: EchoMediaItem?, loadTracks: Boolean)
    abstract fun onIsPlayingChanged(isPlaying: Boolean)

    class Small(
        private val adapter: ShelfAdapter?,
        listener: Listener,
        inflater: LayoutInflater,
        parent: ViewGroup,
        val binding: ItemShelfMediaBinding =
            ItemShelfMediaBinding.inflate(inflater, parent, false)
    ) : MediaItemViewHolder(binding.root) {

        init {
            binding.coverContainer.cover.clipToOutline = true
            binding.more.isVisible = adapter != null
            binding.root.setOnClickListener {
                val tracks = adapter?.getTracks()
                if (tracks == null) listener.onMediaItemClicked(extensionId, item?.media, it)
                else when (val item = item?.media) {
                    is EchoMediaItem.TrackItem -> {
                        val track = item.track
                        val index = tracks.indexOf(track)
                        listener.onTrackClicked(extensionId, tracks, index, null, it)
                    }

                    else -> listener.onMediaItemClicked(extensionId, item, it)
                }
            }
            if (adapter != null) {
                binding.root.setOnLongClickListener {
                    val tracks = adapter.getTracks()
                    when (val item = item?.media) {
                        is EchoMediaItem.TrackItem -> {
                            val track = item.track
                            val index = tracks.indexOf(track)
                            listener.onTrackLongClicked(extensionId, tracks, index, null, it)
                            true
                        }

                        null -> false
                        else -> {
                            listener.onMediaItemLongClicked(extensionId, item, it)
                            true
                        }
                    }
                }

                binding.play.setOnClickListener {
                    listener.onMediaItemPlayClicked(extensionId, item?.media, it)
                }
                binding.more.setOnClickListener {
                    binding.root.performLongClick()
                }
            }
        }

        override fun bind(mediaItem: EchoMediaItem?, loadTracks: Boolean) {
            binding.root.applyTranslationYAnimation(scrollAmount)
            mediaItem ?: return
            binding.bind(mediaItem)
            binding.play.isVisible = adapter != null && mediaItem !is EchoMediaItem.TrackItem
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            binding.coverContainer.isPlaying.isVisible = isPlaying
            (binding.coverContainer.isPlaying.drawable as Animatable).start()
        }
    }

    class TrackCard(
        listener: Listener,
        inflater: LayoutInflater,
        parent: ViewGroup,
        val binding: ItemShelfTrackCardBinding =
            ItemShelfTrackCardBinding.inflate(inflater, parent, false)
    ) : MediaItemViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val track = item?.media as? EchoMediaItem.TrackItem ?: return@setOnClickListener
                val tracks = listOf(track.track)
                listener.onTrackClicked(extensionId, tracks, 0, null, it)
            }
            binding.root.setOnLongClickListener {
                val track =
                    item?.media as? EchoMediaItem.TrackItem ?: return@setOnLongClickListener false
                val tracks = listOf(track.track)
                listener.onTrackLongClicked(extensionId, tracks, 0, null, it)
                true
            }
            binding.more.setOnClickListener {
                binding.root.performLongClick()
            }
            binding.artistCover.setOnClickListener {
                val artists = (item?.media as? EchoMediaItem.TrackItem)?.track?.artists
                    ?: return@setOnClickListener
                val artist = artists.firstOrNull() ?: return@setOnClickListener
                listener.onMediaItemClicked(extensionId, artist.toMediaItem(), it)
            }
            binding.artistCover.clipToOutline = true
            binding.cover.clipToOutline = true
        }

        override fun bind(mediaItem: EchoMediaItem?, loadTracks: Boolean) {
            binding.root.applyTranslationYAnimation(scrollAmount)
            if (mediaItem !is EchoMediaItem.TrackItem) return
            val track = mediaItem.track
            binding.title.text = track.title
            binding.subtitle.text = mediaItem.subtitleWithE
            binding.subtitle.isVisible = !mediaItem.subtitleWithE.isNullOrBlank()
            track.cover.loadInto(binding.cover, R.drawable.art_music)

            val artist = track.artists.firstOrNull()
            binding.artistCover.isVisible = artist != null
            artist?.cover.loadInto(binding.artistCover, R.drawable.art_artist)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            binding.isPlaying.isVisible = isPlaying
            (binding.isPlaying.icon as Animatable).start()
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

        fun ItemShelfMediaBinding.bind(item: EchoMediaItem) {
            title.text = item.title
            val subtitleText = item.subtitleWithDuration()
            subtitle.text = subtitleText
            subtitle.isVisible = !subtitleText.isNullOrBlank()
            coverContainer.run { applyCover(item, cover, listBg1, listBg2, icon) }
        }

        fun getViewType(item: Shelf.Item): Int {
            return if (!item.loadTracks) 0 else when (item.media) {
                is EchoMediaItem.Lists -> 2 //TODO
                is EchoMediaItem.TrackItem -> 1
                else -> 0
            }
        }

        fun create(
            listener: ShelfAdapter.Listener,
            adapter: ShelfAdapter,
            inflater: LayoutInflater,
            parent: ViewGroup,
            viewType: Int
        ) = when (viewType) {
            0 -> Small(adapter, listener, inflater, parent)
            1 -> TrackCard(listener, inflater, parent)
            else -> Small(adapter, listener, inflater, parent)
        }
    }
}