package dev.brahmkshatriya.echo.ui.utils

import android.content.res.Resources

fun Int.dpToPx() = (this * Resources.getSystem().displayMetrics.density).toInt()