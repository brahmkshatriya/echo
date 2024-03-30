package dev.brahmkshatriya.echo.utils

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

fun View.animateTranslation(isRail: Boolean, isVisible: Boolean, duration: Long = 300) {
    clearAnimation()
    val animation =
        if (isRail) animate().translationX(if (isVisible) 0f else -width.toFloat())
        else animate().translationY(if (isVisible) 0f else height.toFloat())
    animation.setInterpolator(AccelerateDecelerateInterpolator())
        .setDuration(duration).start()
}

fun View.animateVisibility(isVisible: Boolean, duration: Long = 300) {
    clearAnimation()
    animate().alpha(if (isVisible) 1f else 0f).setDuration(duration).start()
}