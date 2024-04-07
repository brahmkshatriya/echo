package dev.brahmkshatriya.echo.utils

import android.content.Context.MODE_PRIVATE
import android.util.TypedValue
import android.view.View
import android.view.ViewPropertyAnimator
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.R
import com.google.android.material.motion.MotionUtils
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.ANIMATIONS_KEY
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.SHARED_ELEMENT_KEY

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
        if (animations) {
            fun ViewPropertyAnimator.withEnd(block: () -> Unit) = withEndAction {
                block()
                if (!isVisible) action()
            }

            val animation =
                if (isRail) animate().translationX(value).withEnd { translationX = value }
                else animate().translationY(value).withEnd { translationY = value }
            startAnimation(
                if (isVisible) animation.withStartAction(action)
                else animation
            )
        } else {
            if (isRail) translationX = value
            else translationY = value
            action()
        }
    }

    fun View.animateVisibility(isVisible: Boolean) {
        if (animations) startAnimation(
            animate().alpha(if (isVisible) 1f else 0f)
                .withEndAction { alpha = if (isVisible) 1f else 0f }
        )
        else alpha = if (isVisible) 1f else 0f
    }

    fun animateTranslation(view: View, old: Int, newHeight: Int) = view.run {
        if (view.animations) {
            clearAnimation()
            view.translationY = newHeight.toFloat() - old
            startAnimation(animate().translationY(0f))
        }
    }

    private val View.animations
        get() = context.applicationContext.run {
            getSharedPreferences(packageName, MODE_PRIVATE).getBoolean(ANIMATIONS_KEY, true)
        }

    private val View.sharedElementTransitions
        get() = context.applicationContext.run {
            getSharedPreferences(packageName, MODE_PRIVATE).getBoolean(SHARED_ELEMENT_KEY, true)
        }

    fun Fragment.setupTransition(view: View) {
        if (view.animations) {
            val transitionName = arguments?.getString("transitionName")
            if (transitionName != null && view.sharedElementTransitions) {
                view.transitionName = transitionName
                val value = TypedValue()
                val theme = requireContext().theme
                theme.resolveAttribute(dev.brahmkshatriya.echo.R.attr.echoBackground, value, true)
                val transition = MaterialContainerTransform().apply {
                    drawingViewId = dev.brahmkshatriya.echo.R.id.navHostFragment
                    setAllContainerColors(resources.getColor(value.resourceId, theme))
                    setPathMotion(MaterialArcMotion())
                }
                sharedElementEnterTransition = transition
            }

            exitTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
            reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
            enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
            returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)

            postponeEnterTransition()
            view.doOnPreDraw { startPostponedEnterTransition() }
        }
    }
}