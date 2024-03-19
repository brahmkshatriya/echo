package dev.brahmkshatriya.echo.utils

import kotlinx.coroutines.flow.MutableSharedFlow

fun <T> tryWith(print: Boolean, block: () -> T): T? {
    return try {
        block()
    } catch (e: Throwable) {
        if (print) e.printStackTrace()
        null
    }
}

suspend fun <T> tryWith(block: suspend () -> T): T? {
    return try {
        block()
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}

suspend fun <T> tryWith(throwableFlow:MutableSharedFlow<Throwable>, block: suspend () -> T): T? {
    return try {
        block()
    } catch (e: Throwable) {
        throwableFlow.emit(e)
        null
    }
}