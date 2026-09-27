package dev.brahmkshatriya.echo.app.platform

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform as NativePlatform

class DesktopNativePlatform : Platform {
    @OptIn(ExperimentalNativeApi::class)
    override val name: String = "Kotlin/Native ${NativePlatform.osFamily.name}"
}

actual fun getPlatform(): Platform = DesktopNativePlatform()
