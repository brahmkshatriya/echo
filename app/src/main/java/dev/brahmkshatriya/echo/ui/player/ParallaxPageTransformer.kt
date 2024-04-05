package dev.brahmkshatriya.echo.ui.player

import android.view.View
import androidx.viewpager2.widget.ViewPager2


class ParallaxPageTransformer(private val id: Int) : ViewPager2.PageTransformer {
    private fun parallax(view: View, position: Float) {
        if (position > -1 && position < 1) {
            val width = view.width.toFloat()
            view.translationX = -(position * width)
        }
    }

    override fun transformPage(view: View, position: Float) {
        val bannerContainer = view.findViewById<View>(id)
        parallax(bannerContainer, position)
    }
}