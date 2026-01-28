package dev.brahmkshatriya.echo.platform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform