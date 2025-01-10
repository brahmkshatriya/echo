package dev.brahmkshatriya.echo.utils.ui

import android.content.Context

fun Int.dpToPx(context: Context) = (this * context.resources.displayMetrics.density).toInt()