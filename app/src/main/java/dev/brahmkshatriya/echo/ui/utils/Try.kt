package dev.brahmkshatriya.echo.ui.utils

fun <T> tryWith(block: () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun <T> tryWithSuspend(block: suspend () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}