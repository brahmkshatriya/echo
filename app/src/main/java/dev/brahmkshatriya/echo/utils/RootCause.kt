package dev.brahmkshatriya.echo.utils

val Throwable.rootCause: Throwable
    get() = generateSequence(this) { it.cause }.last()