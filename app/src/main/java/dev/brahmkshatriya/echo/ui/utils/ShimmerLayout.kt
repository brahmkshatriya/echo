package dev.brahmkshatriya.echo.ui.utils

import android.content.Context
import android.util.AttributeSet
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout

class ShimmerLayoutSelf @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    init {
        animation = AlphaAnimation(1.0f, 0.25f).apply {
            duration = 750
            fillAfter = true
            repeatMode = AlphaAnimation.REVERSE
            repeatCount = Animation.INFINITE
        }
    }
}