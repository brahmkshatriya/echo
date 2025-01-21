package dev.brahmkshatriya.echo.common.helpers

fun interface SuspendedFunction {
    suspend operator fun invoke()
}