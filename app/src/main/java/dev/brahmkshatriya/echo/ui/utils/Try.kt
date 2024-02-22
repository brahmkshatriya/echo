package dev.brahmkshatriya.echo.ui.utils

fun <T> tryWith(print: Boolean, block: () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        if (print) e.printStackTrace()
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