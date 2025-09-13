package dev.brahmkshatriya.echo.common.platform

import okio.Path

interface Platform {
    val context: PlatformContext

    val cacheDir: Path

    val downloadDir: Path
}
