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

    fun View.animateTranslation(isRail: Boolean, isVisible: Boolean, action: () -> Unit) {
        val value = if (isVisible) 0f else if (isRail) -width.toFloat() else height.toFloat()
        val animation =
            if (isRail) animate().translationX(value).withEndAction { translationX = value }
            else animate().translationY(value).withEndAction { translationY = value }
        startAnimation(
            if(isVisible) animation.withStartAction(action)
            else animation.withEndAction(action)
        )
    }

    fun View.animateVisibility(isVisible: Boolean) {
        startAnimation(
            animate().alpha(if (isVisible) 1f else 0f)
                .withEndAction { alpha = if (isVisible) 1f else 0f }
        )
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