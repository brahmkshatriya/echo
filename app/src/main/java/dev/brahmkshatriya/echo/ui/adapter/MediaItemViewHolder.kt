package dev.brahmkshatriya.echo.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.databinding.ItemListsCoverBinding
import dev.brahmkshatriya.echo.databinding.ItemProfileCoverBinding
import dev.brahmkshatriya.echo.databinding.ItemTrackCoverBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaListsBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaProfileBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaTitleBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaTrackBinding
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.loadWith

sealed class MediaItemViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: EchoMediaItem)
    abstract val transitionView: View

    class Lists(val binding: NewItemMediaListsBinding) : MediaItemViewHolder(binding.root) {

        private val titleBinding = NewItemMediaTitleBinding.bind(binding.root)
        override val transitionView: View
            get() = binding.cover.listImageContainer

        override fun bind(item: EchoMediaItem) {
            item as EchoMediaItem.Lists
            titleBinding.bind(item)
            binding.cover.bind(item)
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
        private val titleBinding = NewItemMediaTitleBinding.bind(binding.root)

        override val transitionView: View
            get() = binding.cover.root

        override fun bind(item: EchoMediaItem) {
            titleBinding.bind(item)
            binding.cover.bind(item)
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
            get() = binding.cover.root

        override fun bind(item: EchoMediaItem) {
            item as EchoMediaItem.Profile
            binding.title.text = item.title
            binding.subtitle.isVisible = item.subtitle.isNullOrEmpty().not()
            binding.subtitle.text = item.subtitle
            binding.cover.bind(item)
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

        fun EchoMediaItem.icon() = when (this) {
            is EchoMediaItem.TrackItem -> R.drawable.ic_music
            is EchoMediaItem.Profile.ArtistItem -> R.drawable.ic_artist
            is EchoMediaItem.Profile.UserItem -> R.drawable.ic_person
            is EchoMediaItem.Lists.AlbumItem -> R.drawable.ic_album
            is EchoMediaItem.Lists.PlaylistItem -> R.drawable.ic_library_music
        }

        fun NewItemMediaTitleBinding.bind(item: EchoMediaItem) {
            title.text = item.title
            subtitle.isVisible = item.subtitle.isNullOrEmpty().not()
            subtitle.text = item.subtitle
        }

        fun ItemTrackCoverBinding.bind(item: EchoMediaItem) {
            item.cover.loadInto(trackImageView, item.placeHolder())
            this.iconContainer.isVisible = item !is EchoMediaItem.TrackItem
            this.icon.setImageResource(item.icon())
        }

        fun ItemProfileCoverBinding.bind(item: EchoMediaItem) {
            item.cover.loadInto(profileImageView, item.placeHolder())
        }

        fun ItemListsCoverBinding.bind(item: EchoMediaItem.Lists) {
            playlist.isVisible = item is EchoMediaItem.Lists.PlaylistItem
            val cover = item.cover
            cover.loadWith(listImageView) {
                cover.loadInto(listImageView1)
                cover.loadInto(listImageView2)
            }
            albumImage(item.size, listImageContainer1, listImageContainer2)
        }

        private fun albumImage(size: Int?, view1: View, view2: View) {
            val tracks = size ?: 3
            view1.isVisible = tracks > 1
            view2.isVisible = tracks > 2
        }
    }
}
