package dev.brahmkshatriya.echo.common.helpers

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Injectable<T>(
    private val getter: () -> T
) {

    private val _data = lazy { runCatching { getter() } }

    private val mutex = Mutex()
    private val injections = mutableListOf<suspend T.() -> Unit>()
    val value: T?
        get() = _data.value.getOrNull()

    suspend fun value() = runCatching {
        mutex.withLock {
            val t = _data.value.getOrThrow()
            injections.forEach { it(t) }
            injections.clear()
            t
        }
    }

    fun injectIfNotInitialized(block: suspend T.() -> Unit) {
        if (!_data.isInitialized()) injections.add(block)
    }

    suspend fun injectOrRun(block: suspend T.() -> Unit) {
        if (_data.isInitialized()) _data.value.getOrThrow().block()
        else mutex.withLock { injections.add(block) }
    }
}