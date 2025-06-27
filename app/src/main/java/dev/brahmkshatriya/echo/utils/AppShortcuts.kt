package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircleDrawable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

object AppShortcuts {
    private suspend fun Context.applyAppShortcuts(extensions: List<MusicExtension>) {
        val max = ShortcutManagerCompat.getMaxShortcutCountPerActivity(this)
        val ext = extensions.take(max)
        val shortcuts = ext.map { extension ->
            val bitmap =
                extension.metadata.icon.loadAsCircleDrawable(this)
                    ?.toBitmap()?.addPadding()
                    ?: ResourcesCompat.getDrawable(resources, R.drawable.ic_extension, theme)!!
                        .toBitmap()
            ShortcutInfoCompat.Builder(this, extension.id)
                .setShortLabel(extension.name)
                .setIcon(IconCompat.createWithBitmap(bitmap))
                .setIntent(Intent(Intent.ACTION_VIEW, "echo://music/${extension.id}".toUri()))
                .build()
        }
        ShortcutManagerCompat.removeAllDynamicShortcuts(this)
        ShortcutManagerCompat.addDynamicShortcuts(this, shortcuts)
        ShortcutManagerCompat.disableShortcuts(
            this,
            ext.filter { !it.isEnabled }.map { it.id },
            getString(R.string.disabled)
        )
    }

    private fun Bitmap.addPadding(): Bitmap {
        val percentage = 0.05f
        val max = maxOf(width, height)
        val padding = (max * percentage).roundToInt().toFloat()
        val diameter = (max + padding * 2).toInt()
        val newBitmap = createBitmap(diameter, diameter, config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        newBitmap.applyCanvas {
//            canvas.drawColor(getCommonColor())
            canvas.drawBitmap(this@addPadding, padding, padding, null)
        }
        return newBitmap
    }

//    private fun Bitmap.circleCrop(): Bitmap {
//        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
//
//        val minSize = minOf(width, height)
//        val radius = minSize / 2f
//        val output = createBitmap(minSize, minSize, config ?: Bitmap.Config.ARGB_8888)
//        output.applyCanvas {
//            drawCircle(radius, radius, radius, paint)
//            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
//            drawBitmap(this@circleCrop, radius - width / 2f, radius - height / 2f, paint)
//        }
//
//        return output
//    }

//    private fun Bitmap.getCommonColor(): Int {
//        val pixels = IntArray(width * height)
//        getPixels(pixels, 0, width, 0, 0, width, height)
//        val percent = 32
//        val groupedColors = pixels
//            .filter { (it shr 24 and 0xFF) > 64 }
//            .groupBy { color ->
//                val r = (color shr 16 and 0xFF) / percent * percent
//                val g = (color shr 8 and 0xFF) / percent * percent
//                val b = (color and 0xFF) / percent * percent
//                (0xFF shl 24) or (r shl 16) or (g shl 8) or b
//            }
//            .mapKeys { entry -> entry.value.groupBy { it }.maxByOrNull { it.value.size }?.key }
//            .mapValues { it.value.size }
//        return groupedColors.maxByOrNull { it.value }?.key ?: 0
//    }

    fun configureAppShortcuts(loader: ExtensionLoader) {
        val scope = loader.scope
        val musicExt = loader.music
        scope.launch {
            musicExt.collectLatest {
                loader.app.context.applyAppShortcuts(it)
            }
        }
    }
}