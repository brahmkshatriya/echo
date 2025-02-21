package dev.brahmkshatriya.echo.utils

object ExceptionUtils {
    val Throwable.rootCause: Throwable
        get() = generateSequence(this) { it.cause }.last()
}
