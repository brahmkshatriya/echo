package dev.brahmkshatriya.echo.utils

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class SlideInPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        if (position == 0.0f) {
            view.translationY = view.height / 4f
            view.alpha = 0f
            startAnimation(
                view, view.animate().translationY(0f).alpha(1f), 0.5f
            )
        }
    }
}
