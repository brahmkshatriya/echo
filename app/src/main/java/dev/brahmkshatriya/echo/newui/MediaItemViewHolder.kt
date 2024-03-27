package dev.brahmkshatriya.echo.newui
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.databinding.NewItemMediaTrackBinding
import dev.brahmkshatriya.echo.utils.loadInto

@Suppress("LeakingThis")
sealed class MediaItemViewHolder(itemView: View, list: List<EchoMediaItem>) :
    RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: EchoMediaItem)
    open val clickView: View = itemView

    init {
        if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
            val item = list[bindingAdapterPosition]
            clickView.setOnClickListener {
                // Handle click
            }
            clickView.setOnLongClickListener {
                // Handle long click
                true
            }
        }
    }

    class Track(
        val binding: NewItemMediaTrackBinding, list: List<EchoMediaItem>
    ) : MediaItemViewHolder(binding.root,  list) {
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
                parent: ViewGroup,  list: List<EchoMediaItem>
            ): MediaItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Track(
                    NewItemMediaTrackBinding.inflate(layoutInflater, parent, false), list
                )
            }
        }
    }
}