package dev.brahmkshatriya.echo.utils

import android.content.res.Resources

fun Int.dpToPx() = (this * Resources.getSystem().displayMetrics.density).toInt()