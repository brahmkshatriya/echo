package dev.brahmkshatriya.echo.utils

import kotlinx.coroutines.flow.MutableSharedFlow

fun <T> tryWith(print: Boolean, block: () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        if (print) e.printStackTrace()
        null
    }
}

suspend fun <T> tryWith(block: suspend () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun <T> tryWith(exceptionFlow:MutableSharedFlow<Exception>, block: suspend () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        exceptionFlow.emit(e)
        null
    }
}