package dev.brahmkshatriya.echo.ui.shelf.adapter.lists

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfListsMediaBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.PlayerState.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.ui.shelf.adapter.MediaItemViewHolder.Companion.icon
import dev.brahmkshatriya.echo.ui.shelf.adapter.MediaItemViewHolder.Companion.placeHolder
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadWithThumb
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

                else -> listener.onMediaItemClicked(extensionId, item, it)
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
        val gravity = if (item is EchoMediaItem.Profile) Gravity.CENTER else Gravity.NO_GRAVITY
        binding.title.text = item.title
        binding.title.gravity = gravity
        binding.subtitle.text = item.subtitleWithE
        binding.subtitle.gravity = gravity
        binding.subtitle.isVisible = !item.subtitleWithE.isNullOrBlank()
        binding.run { applyCover(item, cover, listBg1, listBg2, icon) }
        binding.root.applyTranslationAndScaleAnimation(xScroll)
    }

    override fun onCurrentChanged(current: PlayerState.Current?) {
        binding.isPlaying.visibility =
            if (current.isPlaying(item?.id)) View.VISIBLE else View.INVISIBLE
        (binding.isPlaying.icon as Animatable).start()
    }

    companion object {
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
            item.cover.loadWithThumb(cover) { image ->
                val drawable = image ?: ResourcesCompat.getDrawable(
                    cover.resources, item.placeHolder, cover.context.theme
                )
                cover.setImageDrawable(drawable)
                when (item) {
                    is EchoMediaItem.Lists -> {
                        val color = image?.toBitmap()?.let {
                            val color = getMergedColor(it)
                            ColorStateList.valueOf(color)
                        }
                        listBg1.backgroundTintList = color
                        listBg2.backgroundTintList = color
                    }

                    is EchoMediaItem.Profile.UserItem -> {
                        icon.isVisible = image == null
                    }

                    else -> Unit
                }
            }
        }

        private fun getMergedColor(bitmap: Bitmap): Int {
            val color = getTopDominantColor(bitmap)
            val grey = Color.GRAY
            return Color.rgb(
                (Color.red(color) + Color.red(grey)) / 2,
                (Color.green(color) + Color.green(grey)) / 2,
                (Color.blue(color) + Color.blue(grey)) / 2
            )
        }

        private fun getTopDominantColor(bitmap: Bitmap): Int {
            val height = (bitmap.height * 0.1f).toInt()
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
    }
}