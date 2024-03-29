package dev.brahmkshatriya.echo.newui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.databinding.NewItemMediaListsBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaProfileBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaTrackBinding
import dev.brahmkshatriya.echo.databinding.NewItemTitleBinding
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.loadWith

sealed class MediaItemViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: EchoMediaItem)
    abstract val transitionView: View

    class Lists(val binding: NewItemMediaListsBinding) : MediaItemViewHolder(binding.root) {

        private val titleBinding = NewItemTitleBinding.bind(binding.root)

        override val transitionView: View
            get() = binding.imageContainer

        override fun bind(item: EchoMediaItem) {
            item as EchoMediaItem.Lists
            titleBinding.bind(item)
            binding.run {
                playlist.isVisible = item is EchoMediaItem.Lists.PlaylistItem
                val cover = item.cover
                cover.loadWith(imageView, item.placeHolder()) {
                    cover.loadInto(imageView1)
                    cover.loadInto(imageView2)
                }
                albumImage(item.size, imageContainer1, imageContainer2)
            }
        }

        companion object {
            fun create(
                parent: ViewGroup
            ): MediaItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Lists(
                    NewItemMediaListsBinding.inflate(layoutInflater, parent, false)
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

    class Profile(val binding: NewItemMediaProfileBinding) : MediaItemViewHolder(binding.root) {
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
                return Profile(
                    NewItemMediaProfileBinding.inflate(layoutInflater, parent, false)
                )
            }
        }
    }


    companion object {
        fun EchoMediaItem.placeHolder() = when (this) {
            is EchoMediaItem.TrackItem -> R.drawable.art_music
            is EchoMediaItem.Profile.ArtistItem -> R.drawable.art_artist
            is EchoMediaItem.Profile.UserItem -> R.drawable.art_user
            is EchoMediaItem.Lists.AlbumItem -> R.drawable.art_album
            is EchoMediaItem.Lists.PlaylistItem -> R.drawable.art_library_music
        }

        fun NewItemTitleBinding.bind(item: EchoMediaItem) {
            title.text = item.title
            subtitle.isVisible = item.subtitle.isNullOrEmpty().not()
            subtitle.text = item.subtitle
        }

        @Suppress("MemberVisibilityCanBePrivate")
        fun albumImage(size: Int?, view1: View, view2: View) {
            val tracks = size ?: 3
            view1.isVisible = tracks > 1
            view2.isVisible = tracks > 2
        }
    }
}
