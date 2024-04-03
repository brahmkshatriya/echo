package dev.brahmkshatriya.echo.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.util.TypedValue
import androidx.palette.graphics.Palette
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.utils.isNightMode

data class PlayerColors(
    val background: Int,
    val clickable: Int,
    val body: Int,
) {
    companion object {
        fun Context.getColorsFrom(bitmap: Bitmap?): PlayerColors? {
            bitmap ?: return null
            val palette = Palette.from(bitmap).generate()
            val lightMode = !isNightMode()
            val lightSwatch = palette.run {
                lightVibrantSwatch ?: vibrantSwatch ?: lightMutedSwatch
            }
            val darkSwatch = palette.run {
                darkVibrantSwatch ?: darkMutedSwatch ?: mutedSwatch
            }
            val bgSwatch = if (lightMode) lightSwatch else darkSwatch
            val clickSwatch = if (lightMode) darkSwatch else lightSwatch
            return bgSwatch?.run {
                PlayerColors(rgb, clickSwatch?.rgb ?: titleTextColor, bodyTextColor)
            }
        }

        fun Context.defaultPlayerColors(): PlayerColors {
            val background = TypedValue()
            theme.resolveAttribute(R.attr.navBackground, background, true)
            val primary = TypedValue()
            theme.resolveAttribute(
                com.google.android.material.R.attr.colorPrimary, primary, true
            )
            val onSurface = TypedValue()
            theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnSurface, onSurface, true
            )
            return PlayerColors(background.data, primary.data, onSurface.data)
        }
    }
}