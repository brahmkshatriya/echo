package dev.brahmkshatriya.echo.utils

import android.content.Context.MODE_PRIVATE
import android.view.View
import android.view.ViewPropertyAnimator
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.color.MaterialColors
import com.google.android.material.motion.MotionUtils
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.ANIMATIONS_KEY
import dev.brahmkshatriya.echo.ui.settings.LookFragment.Companion.SHARED_ELEMENT_KEY

fun startAnimation(
    view: View, animation: ViewPropertyAnimator, durationMultiplier: Float = 1f
) = view.run {
    clearAnimation()
    val interpolator = MotionUtils.resolveThemeInterpolator(
        context, com.google.android.material.R.attr.motionEasingStandardInterpolator,
        FastOutSlowInInterpolator()
    )
    val duration = animationDuration * durationMultiplier
    animation.setInterpolator(interpolator).setDuration(duration.toLong()).start()
}

fun NavigationBarView.animateTranslation(
    isRail: Boolean, isMainFragment: Boolean, visible: Boolean, action: () -> Unit
) {
    val value = if (isMainFragment && visible) 0f
    else if (isRail) -width.toFloat() else height.toFloat()
    if (animations) {
        isVisible = true
        var animation = if (isRail) animate().translationX(value)
        else animate().translationY(value)

        animation =
            if (isMainFragment) animation.withStartAction(action).withEndAction { isVisible = true }
            else animation.withEndAction { action(); isVisible = false }

        startAnimation(this, animation)

        val menuValue =
            if (isMainFragment) 0f else if (isRail) -width.toFloat() else height.toFloat()
        menu.forEachIndexed { index, it ->
            val view = findViewById<View>(it.itemId)
            val dis = menuValue * (index + 1)
            if (isRail) startAnimation(view, view.animate().translationX(dis))
            else startAnimation(view, view.animate().translationY(dis))
        }
    } else {
        isVisible = isMainFragment
        menu.forEach {
            findViewById<View>(it.itemId).apply {
                translationX = 0f
                translationY = 0f
            }
        }
        action()
    }
}

fun View.animateVisibility(visible: Boolean, animate: Boolean = true) {
    if (animations && animate && isVisible != visible) {
        isVisible = true
        startAnimation(
            this,
            animate().alpha(if (visible) 1f else 0f).withEndAction {
                alpha = if (visible) 1f else 0f
                isVisible = visible
            }
        )
    } else {
        alpha = if (visible) 1f else 0f
        isVisible = visible
    }
}

fun animateTranslation(view: View, old: Int, newHeight: Int) = view.run {
    if (view.animations) {
        clearAnimation()
        view.translationY = newHeight.toFloat() - old
        startAnimation(this, animate().translationY(0f))
    }
}

private val View.animationDuration: Long
    get() = context.applicationContext.run {
        MotionUtils.resolveThemeDuration(
            this, com.google.android.material.R.attr.motionDurationMedium1, 350
        ).toLong()
    }
private val View.animations
    get() = context.applicationContext.run {
        getSharedPreferences(packageName, MODE_PRIVATE).getBoolean(ANIMATIONS_KEY, true)
    }

private val View.sharedElementTransitions
    get() = context.applicationContext.run {
        getSharedPreferences(packageName, MODE_PRIVATE).getBoolean(SHARED_ELEMENT_KEY, true)
    }

private val GestureInterpolator = PathInterpolatorCompat.create(0f, 0f, 0f, 1f)

fun Fragment.setupTransition(view: View) {
    val color = MaterialColors.getColor(view, R.attr.echoBackground, 0)
    view.setBackgroundColor(color)

    if (view.animations) {
        val transitionName = arguments?.getString("transitionName")
        if (transitionName != null && view.sharedElementTransitions) {
            view.transitionName = transitionName
            val transition = MaterialContainerTransform().apply {
                drawingViewId = id
                setAllContainerColors(color)
                setPathMotion(MaterialArcMotion())
                duration = view.animationDuration
            }
            sharedElementEnterTransition = transition
        }

        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        if(parentFragmentManager.backStackEntryCount < 1) return
        val predictiveBackMargin = resources.getDimensionPixelSize(R.dimen.predictive_back_margin)
        var initialTouchY = -1f
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    parentFragmentManager.popBackStack()
                }

                override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                    val progress = GestureInterpolator.getInterpolation(backEvent.progress)
                    if(transitionName == null)
                        view.alpha = 1 - progress
                    if (initialTouchY < 0f)
                        initialTouchY = backEvent.touchY
                    val progressY = GestureInterpolator.getInterpolation(
                        (backEvent.touchY - initialTouchY) / view.height
                    )
                    val maxTranslationX = (view.width / 20) - predictiveBackMargin
                    view.translationX = progress * maxTranslationX *
                            (if (backEvent.swipeEdge == BackEventCompat.EDGE_LEFT) -1 else 1)
                    val maxTranslationY = (view.height / 20) - predictiveBackMargin
                    view.translationY = progressY * maxTranslationY * -1
                    val scale = 1f - (0.1f * progress)
                    view.scaleX = scale
                    view.scaleY = scale
                }

                override fun handleOnBackCancelled() {
                    initialTouchY = -1f
                    view.run {
                        translationX = 0f
                        translationY = 0f
                        scaleX = 1f
                        scaleY = 1f
                    }
                }
            }
        )
    }
}