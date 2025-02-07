package dev.brahmkshatriya.echo.utils.ui

import android.content.Context.MODE_PRIVATE
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.color.MaterialColors
import com.google.android.material.motion.MotionUtils
import com.google.android.material.navigation.NavigationBarView
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
    isRail: Boolean,
    isMainFragment: Boolean,
    isPlayerCollapsed: Boolean,
    animate: Boolean = true,
    action: (Float) -> Unit
) = doOnLayout {
    val visible = isMainFragment && isPlayerCollapsed
    val value = if (visible) 0f
    else if (isRail) -width.toFloat() else height.toFloat()
    if (animations && animate) {
        var animation = if (isRail) animate().translationX(value)
        else animate().translationY(value)
        animation = if (visible) animation.withStartAction { action(value) }
        else animation.withEndAction { action(value) }
        startAnimation(this, animation)

        val delay = if (!visible) 0L else animationDurationSmall
        menu.forEachIndexed { index, item ->
            val view = findViewById<View>(item.itemId)
            val anim = view.animate().setStartDelay(index * delay)
            if (isRail) anim.translationX(value)
            else anim.translationY(value)
            startAnimation(view, anim, 0.5f)
        }
    } else {
        if (isRail) translationX = value
        else translationY = value

        menu.forEach {
            findViewById<View>(it.itemId).apply {
                translationX = 0f
                translationY = 0f
            }
        }
        action(value)
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

private val View.animationDurationSmall: Long
    get() = context.applicationContext.run {
        MotionUtils.resolveThemeDuration(
            this, com.google.android.material.R.attr.motionDurationShort1, 100
        ).toLong()
    }

private val View.animations
    get() = context.applicationContext.run {
        getSharedPreferences(packageName, MODE_PRIVATE).getBoolean(ANIMATIONS_KEY, true)
    }

val View.sharedElementTransitions
    get() = context.applicationContext.run {
        getSharedPreferences(packageName, MODE_PRIVATE)
            .getBoolean(SHARED_ELEMENT_KEY, true)
    }

fun Fragment.setupTransition(view: View) {
    val color = MaterialColors.getColor(view, R.attr.echoBackground, 0)
    view.setBackgroundColor(color)

    if (view.animations) {
        val transitionName = arguments?.getString("transitionName")
        if (transitionName != null) {
            view.transitionName = transitionName
            val transition = MaterialContainerTransform().apply {
                drawingViewId = id
                setAllContainerColors(color)
                duration = view.animationDuration
            }
            sharedElementEnterTransition = transition
        }
        (view as? ViewGroup)?.isTransitionGroup = true
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
    }
}