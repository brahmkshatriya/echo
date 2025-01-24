package dev.brahmkshatriya.echo.common.helpers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope

fun interface SuspendedFunction {
    suspend fun CoroutineScope.function()
    suspend operator fun invoke() {
        coroutineScope { function() }
    }
}