package dev.brahmkshatriya.echo.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

const val TIME_MS = 400

fun <T> simpleTween() = tween<T>(TIME_MS, easing = FastOutSlowInEasing)
fun <T> springTween() = tween<T>(TIME_MS, easing = springCubicBezierEasing)

val springCubicBezierEasing = CubicBezierEasing(0.6f, 0f, 0.6f, 1.8f)

