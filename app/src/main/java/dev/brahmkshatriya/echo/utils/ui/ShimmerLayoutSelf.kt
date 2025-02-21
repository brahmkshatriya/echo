package dev.brahmkshatriya.echo.utils.ui

import android.content.Context
import android.util.AttributeSet
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import com.google.android.material.R
import com.google.android.material.motion.MotionUtils

class ShimmerLayoutSelf @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val timeDuration = MotionUtils.resolveThemeDuration(
        context,
        R.attr.motionDurationMedium1,
        350
    ).toLong()

    private val animation = AlphaAnimation(1.0f, 0.25f).apply {
        duration = timeDuration * 2
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