package dev.brahmkshatriya.echo.app.platform

import android.os.Build

class AndroidPlatform : Platform {
    override val name = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()