package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_NO

fun Context.isNightMode() =
    resources.configuration.uiMode and UI_MODE_NIGHT_MASK != UI_MODE_NIGHT_NO