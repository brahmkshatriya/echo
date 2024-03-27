package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.content.res.Resources

fun Int.dpToPx() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun Int.dpToPx(context: Context) = (this * context.resources.displayMetrics.density).toInt()