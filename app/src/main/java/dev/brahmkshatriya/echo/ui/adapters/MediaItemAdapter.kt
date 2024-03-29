package dev.brahmkshatriya.echo.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.MediaItemsContainer
import dev.brahmkshatriya.echo.databinding.ItemMediaAlbumBinding
import dev.brahmkshatriya.echo.databinding.ItemMediaArtistBinding
import dev.brahmkshatriya.echo.databinding.ItemMediaPlaylistBinding
import dev.brahmkshatriya.echo.databinding.ItemMediaTrackBinding
import dev.brahmkshatriya.echo.ui.ClickListener

class MediaItemAdapter(
    private val listener: ClickListener<Pair<View, MediaItemsContainer>>
) : PagingDataAdapter<EchoMediaItem, MediaItemAdapter.MediaItemHolder>(MediaItemComparator) {

    override fun getItemViewType(position: Int): Int {
        return  0
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
                MediaItemsBinding.Album(binding, binding.root)
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

        val mediaItem = item.toMediaItemsContainer()
        holder.itemView.apply {
            setOnClickListener {
                listener.onClick(container.transitionView to mediaItem)
            }
            setOnLongClickListener {
                listener.onLongClick(container.transitionView to mediaItem)
                true
            }
        }

//        when (item) {
//            is EchoMediaItem.TrackItem -> {
//                val binding = (container as MediaItemsBinding.Track).binding
//                binding.title.text = item.track.title
//                item.track.cover.loadInto(binding.imageView, R.drawable.art_music)
//                var subtitle = ""
//                item.track.duration?.toTimeString()?.let {
//                    subtitle += it
//                }
//                item.track.artists.joinToString(", ") { it.name }.let {
//                    if (it.isNotBlank())
//                        subtitle += if (subtitle.isNotBlank()) " • $it" else it
//                }
//                binding.subtitle.isVisible = subtitle.isNotEmpty()
//                binding.subtitle.text = subtitle
//                container.transitionView.transitionName = item.track.id
//            }
//
//            is EchoMediaItem.AlbumItem -> {
//                val binding = (container as MediaItemsBinding.Album).binding
//                binding.title.text = item.album.title
//                item.album.cover.loadWith(binding.imageView, R.drawable.art_album, null) {
//                    item.album.cover.loadInto(binding.imageView1, R.drawable.art_album)
//                    item.album.cover.loadInto(binding.imageView2, R.drawable.art_album)
//                }
//
//                albumImage(item.album.numberOfTracks, binding.imageView1, binding.imageView2)
//                var subtitle = item.album.subtitle ?: ""
//                item.album.numberOfTracks?.let {
//                    subtitle += if (subtitle.isNotBlank()) " • $it" else it
//                }
//                item.album.artists.joinToString(", ") { it.name }.let {
//                    if (it.isNotBlank())
//                        subtitle += if (subtitle.isNotBlank()) " • $it" else it
//                }
//                binding.subtitle.text = subtitle
//                container.transitionView.transitionName = item.album.id
//            }
//
//            is EchoMediaItem.ArtistItem -> {
//                val binding = (container as MediaItemsBinding.Artist).binding
//                binding.title.text = item.artist.name
//                item.artist.cover.loadInto(binding.imageView, R.drawable.art_artist)
//                binding.subtitle.isVisible = !item.artist.subtitle.isNullOrBlank()
//                binding.subtitle.text = item.artist.subtitle
//                container.transitionView.transitionName = item.artist.id
//            }
//
//            is EchoMediaItem.PlaylistItem -> {
//                val binding = (container as MediaItemsBinding.Playlist).binding
//                binding.title.text = item.playlist.title
//                item.playlist.cover.loadWith(
//                    binding.imageView,
//                    R.drawable.art_library_music,
//                    null
//                ) {
//                    item.playlist.cover.loadInto(binding.imageView1, R.drawable.art_library_music)
//                    item.playlist.cover.loadInto(binding.imageView2, R.drawable.art_library_music)
//                }
//
//                binding.subtitle.isVisible = item.playlist.subtitle != null
//                binding.subtitle.text = item.playlist.subtitle
//
//                val tracks = item.playlist.tracks
//                albumImage(tracks.size, binding.imageView1, binding.imageView2)
//                container.transitionView.transitionName = item.playlist.id
//            }
//        }
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

    sealed class MediaItemsBinding(open val transitionView: View) {
        data class Track(val binding: ItemMediaTrackBinding, override val transitionView: View) :
            MediaItemsBinding(transitionView)

        data class Album(val binding: ItemMediaAlbumBinding, override val transitionView: View) :
            MediaItemsBinding(transitionView)

        data class Artist(val binding: ItemMediaArtistBinding, override val transitionView: View) :
            MediaItemsBinding(transitionView)

        data class Playlist(
            val binding: ItemMediaPlaylistBinding,
            override val transitionView: View
        ) :
            MediaItemsBinding(transitionView)
    }

    companion object MediaItemComparator : DiffUtil.ItemCallback<EchoMediaItem>() {
        override fun areItemsTheSame(oldItem: EchoMediaItem, newItem: EchoMediaItem): Boolean {
            return false
        }

        override fun areContentsTheSame(oldItem: EchoMediaItem, newItem: EchoMediaItem): Boolean {
            return oldItem == newItem
        }
    }
}