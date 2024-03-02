package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.util.AttributeSet
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout

class ShimmerLayoutSelf @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    val animation = AlphaAnimation(1.0f, 0.25f).apply {
        duration = 750
        fillAfter = true
        repeatMode = AlphaAnimation.REVERSE
        repeatCount = Animation.INFINITE
    }

    init {
        startAnimation(animation)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        startAnimation(animation)
    }
}