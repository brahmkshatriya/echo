package dev.brahmkshatriya.echo.ui.player

import android.content.Context
import android.graphics.Bitmap
import androidx.palette.graphics.Palette
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.ui.player.PlayerFragment.Companion.showBackground
import dev.brahmkshatriya.echo.utils.ui.UiUtils.isNightMode

data class PlayerColors(
    val background: Int,
    val accent: Int,
    val onAccent: Int,
    val onBackground: Int,
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
            val accentSwatch = if (lightMode) darkSwatch else lightSwatch
            return if (showBackground()) bgSwatch?.run {
                PlayerColors(
                    rgb,
                    accentSwatch?.rgb ?: titleTextColor,
                    accentSwatch?.titleTextColor ?: rgb,
                    bodyTextColor
                )
            } else defaultPlayerColors().let {
                if (accentSwatch != null) PlayerColors(
                    it.background,
                    accentSwatch.rgb,
                    accentSwatch.titleTextColor,
                    it.onBackground
                )
                else it
            }
        }

        fun Context.defaultPlayerColors(): PlayerColors {
            val background = MaterialColors.getColor(
                this, R.attr.navBackground, 0
            )
            val primary = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorPrimary, 0
            )
            val onPrimary = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnPrimary, 0
            )
            val onSurface = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnSurface, 0
            )
            return PlayerColors(background, primary, onPrimary, onSurface)
        }

        fun getDominantColor(bitmap: Bitmap?): Int? {
            bitmap ?: return null
            return Palette.from(bitmap).generate().getDominantColor(0)
        }
    }
}