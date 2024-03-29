package dev.brahmkshatriya.echo.newui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
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
            binding.title.text = item.title

            val subtitle = item.subtitle
            binding.subtitle.isVisible = subtitle.isNullOrEmpty().not()
            binding.subtitle.text = subtitle

            item.cover.loadInto(binding.imageView, item.placeHolder())
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

            fun EchoMediaItem.placeHolder() = when (this) {
                is EchoMediaItem.TrackItem -> R.drawable.art_music
                is EchoMediaItem.AlbumItem -> R.drawable.art_album
                is EchoMediaItem.ArtistItem -> R.drawable.art_artist
                is EchoMediaItem.PlaylistItem -> R.drawable.art_library_music
            }
        }
    }
}