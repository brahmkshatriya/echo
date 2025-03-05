package dev.brahmkshatriya.echo.utils.ui

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.Interpolator
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
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
import com.google.android.material.transition.MaterialSharedAxis
import dev.brahmkshatriya.echo.R

object AnimationUtils {

    private fun startAnimation(
        view: View, animation: ViewPropertyAnimator, durationMultiplier: Float = 1f
    ) = view.run {
        clearAnimation()
        val interpolator = getInterpolator(context)
        val duration = animationDuration * durationMultiplier
        animation.setInterpolator(interpolator).setDuration(duration.toLong()).start()
    }

    private fun getInterpolator(context: Context) = MotionUtils.resolveThemeInterpolator(
        context, com.google.android.material.R.attr.motionEasingStandardInterpolator,
        FastOutSlowInInterpolator()
    )

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

    const val ANIMATIONS_KEY = "animations"
    const val SHARED_ELEMENT_KEY = "shared_element"

    private val View.animations
        get() = context.applicationContext.run {
            getSharedPreferences(packageName, MODE_PRIVATE).getBoolean(ANIMATIONS_KEY, true)
        }

    val View.sharedElementTransitions
        get() = context.applicationContext.run {
            getSharedPreferences(packageName, MODE_PRIVATE)
                .getBoolean(SHARED_ELEMENT_KEY, true)
        }

    fun Fragment.setupTransition(view: View, applyBackground: Boolean = true) {
        if (applyBackground) {
            val color = MaterialColors.getColor(view, R.attr.echoBackground, 0)
            view.setBackgroundColor(color)
        }

        if (view.animations) {
//        val transitionName = arguments?.getString("transitionName")
//        if (transitionName != null) {
//            view.transitionName = transitionName
//            val transition = MaterialContainerTransform().apply {
//                drawingViewId = id
//                setAllContainerColors(color)
//                duration = view.animationDuration
//            }
//            sharedElementEnterTransition = transition
//        }
            (view as? ViewGroup)?.isTransitionGroup = true
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
            reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
            enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
            returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)

            postponeEnterTransition()
            view.doOnPreDraw { startPostponedEnterTransition() }
        }
    }

    private fun animatedWithAlpha(view: View, translate: Animation) =
        AnimationSet(false).apply {
            if (view.animations) {
                val alpha = AlphaAnimation(0.0f, 1.0f)
                val interpolator = getInterpolator(view.context) as Interpolator?
                alpha.duration = view.animationDurationSmall
                alpha.interpolator = interpolator
                addAnimation(alpha)
                translate.duration = view.animationDuration
                translate.interpolator = interpolator
                addAnimation(translate)
            }
        }

    fun View.slideUpAnimation(): AnimationSet {
        val translate = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 1.0f,
            Animation.RELATIVE_TO_SELF, 0f
        )
        return animatedWithAlpha(this, translate)
    }

    fun View.slideInAnimation(): AnimationSet {
        val translate = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 1.0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0f
        )
        return animatedWithAlpha(this, translate)
    }

    fun View.applyScaleAnimation(
        list: List<Float> = listOf(0.5f, 1.0f, 0.5f, 1.0f),
        pivot: Pair<Float, Float> = 0.5f to 0.5f
    ) {
        if (animations) {
            val anim = ScaleAnimation(
                list[0], list[1], list[2], list[3],
                Animation.RELATIVE_TO_SELF, pivot.first,
                Animation.RELATIVE_TO_SELF, pivot.second
            )
            this.startAnimation(animatedWithAlpha(this, anim))
        }
    }
}