package dev.brahmkshatriya.echo.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.databinding.ItemMediaAlbumBinding
import dev.brahmkshatriya.echo.databinding.ItemMediaArtistBinding
import dev.brahmkshatriya.echo.databinding.ItemMediaPlaylistBinding
import dev.brahmkshatriya.echo.databinding.ItemMediaTrackBinding
import dev.brahmkshatriya.echo.player.PlayerHelper.Companion.toTimeString
import dev.brahmkshatriya.echo.ui.ClickListener
import dev.brahmkshatriya.echo.utils.loadInto

class MediaItemAdapter(
    private val listener: ClickListener<Pair<View, EchoMediaItem>>
) : PagingDataAdapter<EchoMediaItem, MediaItemAdapter.MediaItemHolder>(MediaItemComparator) {

    override fun getItemViewType(position: Int): Int {
        return getItem(position)?.let {
            when (it) {
                is EchoMediaItem.TrackItem -> 0
                is EchoMediaItem.AlbumItem -> 1
                is EchoMediaItem.ArtistItem -> 2
                is EchoMediaItem.PlaylistItem -> 3
            }
        } ?: 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MediaItemHolder(
        when (viewType) {
            0 -> {
                val binding = ItemMediaTrackBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                MediaItemsBinding.Track(binding, binding.imageView)
            }

            1 -> {
                val binding = ItemMediaAlbumBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                MediaItemsBinding.Album(binding, binding.imageView)
            }

            2 -> {
                val binding = ItemMediaArtistBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                MediaItemsBinding.Artist(binding, binding.imageView)
            }

            else -> {
                val binding = ItemMediaPlaylistBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                MediaItemsBinding.Playlist(binding, binding.imageView)
            }
        }
    )

    override fun onBindViewHolder(holder: MediaItemHolder, position: Int) {
        val item = getItem(position) ?: return
        val container = holder.container

        holder.itemView.apply {
            setOnClickListener {
                listener.onClick( container.transitionView to item)
            }
            setOnLongClickListener {
                listener.onLongClick(container.transitionView to item)
                true
            }
        }

        when (item) {
            is EchoMediaItem.TrackItem -> {
                val binding = (container as MediaItemsBinding.Track).binding
                binding.title.text = item.track.title
                item.track.cover.loadInto(binding.imageView, R.drawable.art_music)
                var subtitle = ""
                item.track.duration?.toTimeString()?.let {
                    subtitle += it
                }
                item.track.artists.joinToString(", ") { it.name }.let {
                    if (it.isNotBlank())
                        subtitle += if (subtitle.isNotBlank()) " • $it" else it
                }
                binding.subtitle.isVisible = subtitle.isNotEmpty()
                binding.subtitle.text = subtitle
            }

            is EchoMediaItem.AlbumItem -> {
                val binding = (container as MediaItemsBinding.Album).binding
                binding.title.text = item.album.title
                item.album.cover.apply {
                    loadInto(binding.imageView, R.drawable.art_album)
                    loadInto(binding.imageView1)
                    loadInto(binding.imageView2)
                }
                when (item.album.numberOfTracks) {
                    1 -> {
                        binding.imageView1.isVisible = false
                        binding.imageView2.isVisible = false
                    }

                    2 -> {
                        binding.imageView1.isVisible = true
                        binding.imageView2.isVisible = false
                    }

                    else -> {
                        binding.imageView1.isVisible = true
                        binding.imageView2.isVisible = true
                    }
                }
                var subtitle = item.album.numberOfTracks.toString()
                item.album.artists.joinToString(", ") { it.name }.let {
                    if (it.isNotBlank())
                        subtitle += if (subtitle.isNotBlank()) " • $it" else it
                }
                binding.subtitle.text = subtitle
            }

            is EchoMediaItem.ArtistItem -> {
                val binding = (container as MediaItemsBinding.Artist).binding
                binding.title.text = item.artist.name
                item.artist.cover.loadInto(binding.imageView, R.drawable.art_artist)
                binding.subtitle.isVisible = item.artist.subtitle.isNullOrBlank()
                binding.subtitle.text = item.artist.subtitle
            }

            is EchoMediaItem.PlaylistItem -> {
                val binding = (container as MediaItemsBinding.Playlist).binding
                binding.title.text = item.playlist.title
                item.playlist.cover?.apply {
                    loadInto(binding.imageView, R.drawable.art_library_music)
                    loadInto(binding.imageView1)
                    loadInto(binding.imageView2)
                }

                binding.subtitle.isVisible = item.playlist.subtitle != null
                binding.subtitle.text = item.playlist.subtitle

                val tracks = (item.playlist as? Playlist.Full)?.tracks ?: emptyList()
                when (tracks.size) {
                    1 -> {
                        binding.imageView1.isVisible = false
                        binding.imageView2.isVisible = false
                    }

                    2 -> {
                        binding.imageView1.isVisible = true
                        binding.imageView2.isVisible = false
                    }

                    else -> {
                        binding.imageView1.isVisible = true
                        binding.imageView2.isVisible = true
                    }
                }
            }
        }
    }

    fun submitData(lifecycle: Lifecycle, list: List<EchoMediaItem>) {
        submitData(lifecycle, PagingData.from(list))
    }

    inner class MediaItemHolder(val container: MediaItemsBinding) :
        RecyclerView.ViewHolder(
            when (container) {
                is MediaItemsBinding.Track -> container.binding.root
                is MediaItemsBinding.Album -> container.binding.root
                is MediaItemsBinding.Artist -> container.binding.root
                is MediaItemsBinding.Playlist -> container.binding.root
            }
        )

    sealed class MediaItemsBinding(open val transitionView: ImageView) {
        data class Track(val binding: ItemMediaTrackBinding, override val transitionView: ImageView) :
            MediaItemsBinding(transitionView)

        data class Album(val binding: ItemMediaAlbumBinding, override val transitionView: ImageView) :
            MediaItemsBinding(transitionView)

        data class Artist(val binding: ItemMediaArtistBinding, override val transitionView: ImageView) :
            MediaItemsBinding(transitionView)

        data class Playlist(val binding: ItemMediaPlaylistBinding, override val transitionView: ImageView) :
            MediaItemsBinding(transitionView)
    }

    companion object MediaItemComparator : DiffUtil.ItemCallback<EchoMediaItem>() {
        override fun areItemsTheSame(oldItem: EchoMediaItem, newItem: EchoMediaItem): Boolean {
            if (oldItem is EchoMediaItem.TrackItem && newItem is EchoMediaItem.TrackItem)
                return oldItem.track.uri == newItem.track.uri
            if (oldItem is EchoMediaItem.AlbumItem && newItem is EchoMediaItem.AlbumItem)
                return oldItem.album.uri == newItem.album.uri
            if (oldItem is EchoMediaItem.ArtistItem && newItem is EchoMediaItem.ArtistItem)
                return oldItem.artist.uri == newItem.artist.uri
            if (oldItem is EchoMediaItem.PlaylistItem && newItem is EchoMediaItem.PlaylistItem)
                return oldItem.playlist.uri == newItem.playlist.uri
            return false
        }

        override fun areContentsTheSame(oldItem: EchoMediaItem, newItem: EchoMediaItem): Boolean {
            return oldItem == newItem
        }
    }
}