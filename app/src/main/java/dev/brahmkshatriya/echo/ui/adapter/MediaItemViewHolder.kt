package dev.brahmkshatriya.echo.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.databinding.ItemCoverListsBinding
import dev.brahmkshatriya.echo.databinding.ItemCoverProfileBinding
import dev.brahmkshatriya.echo.databinding.ItemCoverTrackBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaListsBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaProfileBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaTitleBinding
import dev.brahmkshatriya.echo.databinding.NewItemMediaTrackBinding
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.playback.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.utils.image.loadInto
import dev.brahmkshatriya.echo.utils.image.loadWithBitmap
import dev.brahmkshatriya.echo.utils.ui.animateVisibility


sealed class MediaItemViewHolder(
    val listener: ShelfAdapter.Listener,
    val clientId: String,
    itemView: View
) : ShelfListItemViewHolder(itemView) {
    abstract fun bind(item: EchoMediaItem)

    override fun bind(item: Any) {
        if (item !is EchoMediaItem) return
        bind(item)
        itemView.setOnClickListener {
            listener.onClick(clientId, item, transitionView)
        }
        itemView.setOnLongClickListener {
            listener.onLongClick(clientId, item, transitionView)
        }
    }

    abstract override val transitionView: View

    class Lists(
        listener: ShelfAdapter.Listener,
        clientId: String,
        val binding: NewItemMediaListsBinding
    ) : MediaItemViewHolder(listener, clientId, binding.root) {

        private val titleBinding = NewItemMediaTitleBinding.bind(binding.root)
        override val transitionView: View
            get() = binding.cover.root

        override fun onCurrentChanged(current: Current?) {
            applyIsPlaying(current, item?.id, binding.cover.isPlaying)
        }

        var item: EchoMediaItem? = null
        var isPlaying: MaterialButton? = null
        override fun bind(item: EchoMediaItem) {
            item as EchoMediaItem.Lists
            this.item = item
            titleBinding.bind(item)
            isPlaying = binding.cover.bind(item)
        }

        companion object {
            fun create(
                listener: ShelfAdapter.Listener,
                clientId: String,
                parent: ViewGroup
            ): MediaItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Lists(
                    listener, clientId,
                    NewItemMediaListsBinding.inflate(layoutInflater, parent, false)
                )
            }
        }
    }

    class Track(
        listener: ShelfAdapter.Listener,
        clientId: String,
        val binding: NewItemMediaTrackBinding
    ) : MediaItemViewHolder(listener, clientId, binding.root) {
        private val titleBinding = NewItemMediaTitleBinding.bind(binding.root)

        override val transitionView: View
            get() = binding.cover.root

        override fun onCurrentChanged(current: Current?) {
            applyIsPlaying(current, item?.id, binding.cover.isPlaying)
        }

        var item: EchoMediaItem? = null
        var isPlaying: MaterialButton? = null
        override fun bind(item: EchoMediaItem) {
            this.item = item
            titleBinding.bind(item)
            isPlaying = binding.cover.bind(item)
        }

        companion object {
            fun create(
                listener: ShelfAdapter.Listener,
                clientId: String,
                parent: ViewGroup
            ): MediaItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Track(
                    listener, clientId,
                    NewItemMediaTrackBinding.inflate(layoutInflater, parent, false)
                )
            }
        }
    }

    class Profile(
        listener: ShelfAdapter.Listener,
        clientId: String,
        val binding: NewItemMediaProfileBinding
    ) : MediaItemViewHolder(listener, clientId, binding.root) {
        override val transitionView: View
            get() = binding.cover.root

        override fun onCurrentChanged(current: Current?) {
            applyIsPlaying(current, item?.id, binding.cover.isPlaying)
        }

        var item: EchoMediaItem? = null
        var isPlaying: MaterialButton? = null
        override fun bind(item: EchoMediaItem) {
            item as EchoMediaItem.Profile
            this.item = item
            binding.title.text = item.title
            binding.subtitle.isVisible = item.subtitleWithE.isNullOrEmpty().not()
            binding.subtitle.text = item.subtitleWithE
            isPlaying = binding.cover.bind(item)
        }

        companion object {
            fun create(
                listener: ShelfAdapter.Listener,
                clientId: String,
                parent: ViewGroup
            ): MediaItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                return Profile(
                    listener, clientId,
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
            is EchoMediaItem.Lists.RadioItem -> R.drawable.art_sensors
        }

        fun EchoMediaItem.icon() = when (this) {
            is EchoMediaItem.TrackItem -> R.drawable.ic_music
            is EchoMediaItem.Profile.ArtistItem -> R.drawable.ic_artist
            is EchoMediaItem.Profile.UserItem -> R.drawable.ic_person
            is EchoMediaItem.Lists.AlbumItem -> R.drawable.ic_album
            is EchoMediaItem.Lists.PlaylistItem -> R.drawable.ic_library_music
            is EchoMediaItem.Lists.RadioItem -> R.drawable.ic_sensors
        }

        fun NewItemMediaTitleBinding.bind(item: EchoMediaItem) {
            title.text = item.title
            subtitle.isVisible = item.subtitleWithE.isNullOrEmpty().not()
            subtitle.text = item.subtitleWithE
        }

        private fun View.toolTipOnClick() {
            setOnClickListener { performLongClick() }
        }

        fun applyIsPlaying(
            current: Current?, id: String?, view: MaterialButton?
        ) {
            view ?: return
            view.toolTipOnClick()
            view.isVisible = false
            val playing = current.isPlaying(id)
            view.animateVisibility(playing)
            if (playing) (view.icon as Animatable).start()
        }

        fun ItemCoverTrackBinding.bind(item: EchoMediaItem): MaterialButton {
            trackImageView.clipToOutline = true
            item.cover.loadInto(trackImageView, item.placeHolder())
            return isPlaying
        }

        fun ItemCoverProfileBinding.bind(item: EchoMediaItem): MaterialButton {
            profileImageView.clipToOutline = true
            item.cover.loadInto(profileImageView, item.placeHolder())
            return isPlaying
        }

        fun ItemCoverListsBinding.bind(item: EchoMediaItem): MaterialButton {
            playlist.isVisible = item is EchoMediaItem.Lists.PlaylistItem
            listImageView.clipToOutline = true
            item.cover.loadWithBitmap(listImageView, item.placeHolder()) { bitmap ->
                val tint = bitmap?.let { ColorStateList.valueOf(getDominantColor(it)) }
                listImageContainer1.backgroundTintList = tint
                listImageContainer2.backgroundTintList = tint
            }
            val size = (item as? EchoMediaItem.Lists)?.size
            albumImage(size, listImageContainer1, listImageContainer2)
            return isPlaying
        }

        private fun getDominantColor(bitmap: Bitmap): Int {
            val height = bitmap.height
            val pixels = IntArray(bitmap.width * height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, height)

            var redBucket = 0
            var greenBucket = 0
            var blueBucket = 0
            var alphaBucket = 0

            val hasAlpha = bitmap.hasAlpha()
            val pixelCount = pixels.size

            pixels.forEach { color ->
                redBucket += (color shr 16) and 0xFF
                greenBucket += (color shr 8) and 0xFF
                blueBucket += color and 0xFF
                if (hasAlpha) alphaBucket += color ushr 24
            }

            return Color.argb(
                if (hasAlpha) alphaBucket / pixelCount else 255,
                redBucket / pixelCount,
                greenBucket / pixelCount,
                blueBucket / pixelCount
            )
        }

        private fun albumImage(size: Int?, view1: View, view2: View) {
            val tracks = size ?: 3
            view1.isVisible = tracks > 1
            view2.isVisible = tracks > 2
        }

        fun create(
            viewType: Int, parent: ViewGroup,
            listener: ShelfAdapter.Listener,
            clientId: String,
        ): ShelfListItemViewHolder {
            return when (viewType) {
                0 -> Lists.create(listener, clientId, parent)
                1 -> Profile.create(listener, clientId, parent)
                2 -> Track.create(listener, clientId, parent)
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }

        fun getViewType(item: EchoMediaItem) = when (item) {
            is EchoMediaItem.Lists.RadioItem -> 2
            is EchoMediaItem.Lists -> 0
            is EchoMediaItem.Profile -> 1
            is EchoMediaItem.TrackItem -> 2
        }
    }
}
