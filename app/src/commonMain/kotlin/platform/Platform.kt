package dev.brahmkshatriya.echo.app.platform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform