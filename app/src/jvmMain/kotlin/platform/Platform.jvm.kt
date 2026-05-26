package dev.brahmkshatriya.echo.app.platform

class JVMPlatform: Platform {
    override val name = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()