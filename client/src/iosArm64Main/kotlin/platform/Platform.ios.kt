package dev.brahmkshatriya.echo.app.platform

import platform.UIKit.UIDevice

private class IosPlatform : Platform {
    override val name: String = buildString {
        append(UIDevice.currentDevice.systemName)
        append(' ')
        append(UIDevice.currentDevice.systemVersion)
    }
}

actual fun getPlatform(): Platform = IosPlatform()
