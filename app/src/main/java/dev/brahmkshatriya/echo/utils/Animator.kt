package dev.brahmkshatriya.echo.utils

import android.view.View
import android.view.ViewPropertyAnimator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.motion.MotionUtils

object Animator {

    private fun View.startAnimation(animation: ViewPropertyAnimator) {
        clearAnimation()
        val interpolator = MotionUtils.resolveThemeInterpolator(
            context,
            R.attr.motionEasingStandardInterpolator,
            FastOutSlowInInterpolator()
        )
        val duration = MotionUtils.resolveThemeDuration(
            context,
            R.attr.motionDurationLong1,
            500
        ).toLong()
        animation.setInterpolator(interpolator).setDuration(duration).start()
    }

    fun View.animateTranslation(isRail: Boolean, isVisible: Boolean) {
        val animation =
            if (isRail) animate().translationX(if (isVisible) 0f else -width.toFloat())
            else animate().translationY(if (isVisible) 0f else height.toFloat())
        startAnimation(animation)
    }

    fun View.animateVisibility(isVisible: Boolean) {
        startAnimation(animate().alpha(if (isVisible) 1f else 0f))
    }

    fun BottomSheetBehavior<View>.animatePeekHeight(view: View, newHeight: Int) = view.run {
        clearAnimation()
        view.translationY = newHeight.toFloat() - peekHeight
        peekHeight = newHeight
        startAnimation(animate().translationY(0f))
    }
}