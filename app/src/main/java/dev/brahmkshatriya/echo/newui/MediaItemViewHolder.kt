package dev.brahmkshatriya.echo.newui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.databinding.NewItemCoversBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaAlbumBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaArtistBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaPlaylistBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaTrackBinding
import dev.brahmkshatriya.echo.databinding.NewItemTitleBinding
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.loadWith

sealed class MediaItemViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: EchoMediaItem)
    abstract val transitionView: View

    class Album(val binding: NewItemMediaAlbumBinding) : MediaItemViewHolder(binding.root) {
        private val titleBinding = NewItemTitleBinding.bind(binding.root)
        private val coversBinding = NewItemCoversBinding.bind(binding.root)

        override val transitionView: View
            get() = coversBinding.imageContainer

        override fun bind(item: EchoMediaItem) {
            titleBinding.bind(item)
            coversBinding.bind(item)
        }

        companion object {
            fun create(
                parent: ViewGroup
            ): MediaItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Album(
                    NewItemMediaAlbumBinding.inflate(layoutInflater, parent, false)
                )
            }
        }
    }

    class Playlist(val binding: NewItemMediaPlaylistBinding) : MediaItemViewHolder(binding.root) {

        private val titleBinding = NewItemTitleBinding.bind(binding.root)
        private val coversBinding = NewItemCoversBinding.bind(binding.root)

        override val transitionView: View
            get() = coversBinding.imageContainer

        override fun bind(item: EchoMediaItem) {
            titleBinding.bind(item)
            coversBinding.bind(item)
        }

        companion object {
            fun create(
                parent: ViewGroup
            ): MediaItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Playlist(
                    NewItemMediaPlaylistBinding.inflate(layoutInflater, parent, false)
                )
            }
        }
    }

    class Track(val binding: NewItemMediaTrackBinding) : MediaItemViewHolder(binding.root) {
        private val titleBinding = NewItemTitleBinding.bind(binding.root)

        override val transitionView: View
            get() = binding.imageContainer

        override fun bind(item: EchoMediaItem) {
            item.cover.loadInto(binding.imageView, item.placeHolder())
            titleBinding.bind(item)
        }

        companion object {
            fun create(
                parent: ViewGroup
            ): MediaItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Track(
                    NewItemMediaTrackBinding.inflate(layoutInflater, parent, false)
                )
            }
        }
    }

    class Artist(val binding: NewItemMediaArtistBinding) : MediaItemViewHolder(binding.root) {
        override val transitionView: View
            get() = binding.imageContainer

        override fun bind(item: EchoMediaItem) {
            binding.title.text = item.title
            binding.subtitle.isVisible = item.subtitle.isNullOrEmpty().not()
            binding.subtitle.text = item.subtitle
            item.cover.loadInto(binding.imageView, item.placeHolder())
        }

        companion object {
            fun create(
                parent: ViewGroup
            ): MediaItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Artist(
                    NewItemMediaArtistBinding.inflate(layoutInflater, parent, false)
                )
            }
        }
    }


    companion object {
        fun EchoMediaItem.placeHolder() = when (this) {
            is EchoMediaItem.TrackItem -> R.drawable.art_music
            is EchoMediaItem.AlbumItem -> R.drawable.art_album
            is EchoMediaItem.ArtistItem -> R.drawable.art_artist
            is EchoMediaItem.PlaylistItem -> R.drawable.art_library_music
        }

        fun NewItemTitleBinding.bind(item: EchoMediaItem) {
            title.text = item.title
            subtitle.isVisible = item.subtitle.isNullOrEmpty().not()
            subtitle.text = item.subtitle
        }

        fun NewItemCoversBinding.bind(item: EchoMediaItem) {
            val cover = item.cover
            cover.loadWith(imageView, item.placeHolder()) {
                cover.loadInto(imageView1)
                cover.loadInto(imageView2)
            }
            albumImage(item, imageView1, imageView2)
        }

        @Suppress("MemberVisibilityCanBePrivate")
        fun albumImage(item: EchoMediaItem, view1: View, view2: View) {
            val size = when (item) {
                is EchoMediaItem.AlbumItem -> item.album.numberOfTracks
                    ?: item.album.tracks.ifEmpty { null }?.size
                is EchoMediaItem.PlaylistItem ->
                    item.playlist.tracks.ifEmpty { null }?.size
                else -> null
            } ?: 3
            view1.isVisible = size > 1
            view2.isVisible = size > 2
        }
    }
}
