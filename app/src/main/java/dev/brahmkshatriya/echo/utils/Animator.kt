package dev.brahmkshatriya.echo.utils

import android.util.TypedValue
import android.view.View
import android.view.ViewPropertyAnimator
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.motion.MotionUtils
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform

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

    fun Fragment.setupTransition(view: View) {
        val value = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(dev.brahmkshatriya.echo.R.attr.echoBackground, value, true)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = dev.brahmkshatriya.echo.R.id.navHostFragment
            setAllContainerColors(resources.getColor(value.resourceId, theme))
            setPathMotion(MaterialArcMotion())
        }
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
    }
}