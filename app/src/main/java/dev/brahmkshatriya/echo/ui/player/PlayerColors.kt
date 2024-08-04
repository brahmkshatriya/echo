package dev.brahmkshatriya.echo.ui.player

import android.content.Context
import android.graphics.Bitmap
import androidx.palette.graphics.Palette
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.isNightMode

data class PlayerColors(
    val background: Int,
    val accent: Int,
    val text: Int,
) {
    companion object {
        fun Context.getColorsFrom(bitmap: Bitmap): PlayerColors? {
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
            val background = MaterialColors.getColor(
                this, R.attr.navBackground, 0
            )
            val primary = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorPrimary, 0
            )
            val onSurface = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnSurface, 0
            )
            return PlayerColors(background, primary, onSurface)
        }
    }
}