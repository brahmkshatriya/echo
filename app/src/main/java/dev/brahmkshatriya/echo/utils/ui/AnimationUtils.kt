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
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import androidx.core.view.doOnLayout
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
import dev.brahmkshatriya.echo.utils.ContextUtils.SETTINGS_NAME
import kotlin.math.absoluteValue
import kotlin.math.sign

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
    const val SCROLL_ANIMATIONS_KEY = "shared_element"

    private val View.animations
        get() = context.applicationContext.run {
            getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE).getBoolean(ANIMATIONS_KEY, true)
        }

    private val View.scrollAnimations
        get() = context.applicationContext.run {
            getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE)
                .getBoolean(SCROLL_ANIMATIONS_KEY, false)
        }

    fun Fragment.setupTransition(
        view: View, applyBackground: Boolean = true, useZ: Boolean = true
    ) {
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
            val axis = if (useZ) MaterialSharedAxis.Z else MaterialSharedAxis.X
            exitTransition = MaterialSharedAxis(axis, true)
            reenterTransition = MaterialSharedAxis(axis, false)
            enterTransition = MaterialSharedAxis(axis, true)
            returnTransition = MaterialSharedAxis(axis, false)

//            postponeEnterTransition()
//            view.doOnPreDraw { startPostponedEnterTransition() }
        }
    }

    private fun View.animatedWithAlpha(delay: Long = 0, vararg anim: Animation) {
        if (!animations) return
        val set = AnimationSet(true)
        set.interpolator = getInterpolator(context) as Interpolator
        val alpha = AlphaAnimation(0.0f, 1.0f)
        alpha.duration = animationDurationSmall
        alpha.startOffset = delay
        set.addAnimation(alpha)
        anim.forEach { set.addAnimation(it) }
        startAnimation(set)
    }

    fun View.applyTranslationAndScaleAnimation(
        amount: Int, delay: Long = 0
    ) {
        if (!animations) return
        val multiplier = amount.sign
        val rotateAnimation = RotateAnimation(
            5f * multiplier, 0f,
            width.toFloat() / 2, height.toFloat() / 2
        )
        rotateAnimation.duration = animationDuration
        val translate = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, multiplier * 0.5f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f,
        )
        translate.duration = animationDuration
        val from = 1f - 0.5f * multiplier.absoluteValue
        val scale = ScaleAnimation(
            from, 1f, from, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        scale.duration = animationDuration
        animatedWithAlpha(delay, rotateAnimation, translate, scale)
    }

    fun View.applyTranslationYAnimation(amount: Int, delay: Long = 0) {
        if (!animations) return
        if (!scrollAnimations) return
        val multiplier = amount.sign
        val translate = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, multiplier * 1.5f,
            Animation.RELATIVE_TO_SELF, 0f,
        )
        translate.duration = animationDuration
        translate.startOffset = delay
        animatedWithAlpha(delay, translate)
    }
}