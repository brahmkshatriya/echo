package dev.brahmkshatriya.echo.extensions.plugger

import android.content.pm.ApplicationInfo

data class AppInfo(
    val path: String,
    val appInfo: ApplicationInfo
)