package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.content.Intent

fun Context.restartApp() {
    val mainIntent = Intent.makeRestartActivityTask(
        packageManager.getLaunchIntentForPackage(packageName)!!.component
    )
    startActivity(mainIntent)
    Runtime.getRuntime().exit(0)
}