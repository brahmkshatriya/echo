package dev.brahmkshatriya.echo.ui.shelf.adapter.lists

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.databinding.ItemShelfListsMediaBinding
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.playback.PlayerState.Current.Companion.isPlaying
import dev.brahmkshatriya.echo.ui.shelf.adapter.MediaItemShelfViewHolder.Companion.icon
import dev.brahmkshatriya.echo.ui.shelf.adapter.MediaItemShelfViewHolder.Companion.placeHolder
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfAdapter
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadWithThumb

class MediaItemShelfListsViewHolder(
    val listener: ShelfListsAdapter.Listener,
    val binding: ItemShelfListsMediaBinding
) : ShelfListsAdapter.ViewHolder(binding.root) {

    var item: EchoMediaItem? = null

    init {
        binding.root.setOnClickListener {
            listener.onMediaItemClicked(extensionId, item, it)
        }
        binding.root.setOnLongClickListener {
            listener.onMediaItemLongClicked(extensionId, item, it)
            true
        }
        binding.cover.clipToOutline = true
    }

    override fun bind(shelf: Shelf.Lists<*>?, position: Int) {
        val items = (shelf as? Shelf.Lists.Items)?.list ?: return
        val item = items.getOrNull(position) ?: return
        this.item = item
        val gravity = if (item is EchoMediaItem.Profile) Gravity.CENTER else Gravity.NO_GRAVITY
        binding.title.text = item.title
        binding.title.gravity = gravity
        binding.subtitle.text = item.subtitleWithE
        binding.subtitle.gravity = gravity
        binding.subtitle.isVisible = !item.subtitleWithE.isNullOrBlank()
        item.cover.loadWithThumb(binding.cover) { image ->
            val drawable = image ?: ResourcesCompat.getDrawable(
                binding.root.resources, item.placeHolder, binding.root.context.theme
            )
            binding.cover.setImageDrawable(drawable)
            if (item !is EchoMediaItem.Lists) return@loadWithThumb
            val color = image?.toBitmap()?.let {
                val color = getTopDominantColor(it)
                ColorStateList.valueOf(color)
            }
            binding.listBg1.backgroundTintList = color
            binding.listBg2.backgroundTintList = color
        }
        binding.cover.setBackgroundResource(
            if (item is EchoMediaItem.Profile) R.drawable.rounded_rectangle_cover_profile
            else R.drawable.rounded_rectangle_cover
        )
        val bgVisible = item is EchoMediaItem.Lists
        binding.listBg1.isVisible = bgVisible
        binding.listBg2.isVisible = bgVisible
        binding.icon.isVisible = bgVisible && item !is EchoMediaItem.Lists.AlbumItem
        binding.icon.setImageResource(item.icon)
    }

    override fun onCurrentChanged(current: PlayerState.Current?) {
        binding.isPlaying.visibility =
            if (current.isPlaying(item?.id)) View.VISIBLE else View.INVISIBLE
        (binding.isPlaying.icon as Animatable).start()
    }

    companion object {
        fun create(
            listener: ShelfAdapter.Listener, inflater: LayoutInflater, parent: ViewGroup
        ): MediaItemShelfListsViewHolder {
            val binding = ItemShelfListsMediaBinding.inflate(inflater, parent, false)
            return MediaItemShelfListsViewHolder(listener, binding)
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