package dev.brahmkshatriya.echo.ui.shelf.adapter.lists

import android.graphics.drawable.Animatable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfListsMediaBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.PlayerState.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.ui.shelf.adapter.MediaItemViewHolder.Companion.icon
import dev.brahmkshatriya.echo.ui.shelf.adapter.MediaItemViewHolder.Companion.placeHolder
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadInto
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyTranslationAndScaleAnimation

class MediaItemShelfListsViewHolder(
    listener: ShelfListsAdapter.Listener,
    inflater: LayoutInflater,
    parent: ViewGroup,
    val binding: ItemShelfListsMediaBinding =
        ItemShelfListsMediaBinding.inflate(inflater, parent, false)
) : ShelfListsAdapter.ViewHolder(binding.root) {

    var item: EchoMediaItem? = null

    init {
        binding.root.setOnClickListener {
            when (val item = item) {
                is EchoMediaItem.TrackItem -> listener.onTrackClicked(
                    extensionId, listOf(item.track), 0, null, it
                )

                is EchoMediaItem.Lists.RadioItem ->
                    listener.onMediaItemPlayClicked(extensionId, item, it)

                else ->
                    listener.onMediaItemClicked(extensionId, item, it)
            }
        }
        binding.root.setOnLongClickListener {
            when (val item = item) {
                is EchoMediaItem.TrackItem -> listener.onTrackLongClicked(
                    extensionId, listOf(item.track), 0, null, it
                )

                else -> listener.onMediaItemLongClicked(extensionId, item, it)
            }
            true
        }
        binding.cover.clipToOutline = true
    }

    override fun bind(shelf: Shelf.Lists<*>?, position: Int, xScroll: Int, yScroll: Int) {
        val items = (shelf as? Shelf.Lists.Items)?.list ?: return
        val item = items.getOrNull(position) ?: return
        this.item = item
        binding.bind(item)
        binding.root.applyTranslationAndScaleAnimation(xScroll)
    }

    override fun onCurrentChanged(current: PlayerState.Current?) {
        binding.isPlaying.visibility =
            if (current.isPlaying(item?.id)) View.VISIBLE else View.INVISIBLE
        (binding.isPlaying.icon as Animatable).start()
    }

    companion object {

        fun ItemShelfListsMediaBinding.bind(item: EchoMediaItem) {
            val gravity = if (item is EchoMediaItem.Profile) Gravity.CENTER else Gravity.NO_GRAVITY
            title.text = item.title
            title.gravity = gravity
            subtitle.text = item.subtitleWithE
            subtitle.gravity = gravity
            subtitle.isVisible = !item.subtitleWithE.isNullOrBlank()
            applyCover(item, cover, listBg1, listBg2, icon)
        }

        fun applyCover(
            item: EchoMediaItem,
            cover: ImageView,
            listBg1: View,
            listBg2: View,
            icon: ImageView,
        ) {
            icon.isVisible = when (item) {
                is EchoMediaItem.TrackItem -> false
                is EchoMediaItem.Profile.ArtistItem -> false
                is EchoMediaItem.Lists.AlbumItem -> false
                else -> true
            }
            icon.setImageResource(item.icon)
            cover.setBackgroundResource(
                if (item is EchoMediaItem.Profile) R.drawable.rounded_rectangle_cover_profile
                else R.drawable.rounded_rectangle_cover
            )
            val bgVisible = item is EchoMediaItem.Lists
            listBg1.isVisible = bgVisible
            listBg2.isVisible = bgVisible
            item.cover.loadInto(cover, item.placeHolder)
        }
    }
}