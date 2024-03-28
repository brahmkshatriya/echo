package dev.brahmkshatriya.echo.newui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.databinding.NewItemMediaTrackBinding
import dev.brahmkshatriya.echo.utils.loadInto

sealed class MediaItemViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: EchoMediaItem)
    open val clickView: View = itemView
    open val transitionView: View
        get() = this.clickView

    class Track(val binding: NewItemMediaTrackBinding) : MediaItemViewHolder(binding.root) {

        override val transitionView: View
            get() = binding.imageContainer

        override fun bind(item: EchoMediaItem) {

            val (title, cover) = when (item) {
                is EchoMediaItem.AlbumItem -> {
                    val album = item.album
                    album.title to album.cover
                }

                is EchoMediaItem.ArtistItem -> {
                    val artist = item.artist
                    artist.name to artist.cover
                }

                is EchoMediaItem.PlaylistItem -> {
                    val playlist = item.playlist
                    playlist.title to playlist.cover
                }

                is EchoMediaItem.TrackItem -> {
                    val track = item.track
                    track.title to track.cover
                }
            }

            binding.title.text = title
            cover.loadInto(binding.imageView)
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
}