package dev.brahmkshatriya.echo.common.helpers

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Injectable<T>(
    private val getter: () -> T
) {

    private val _data = lazy { runCatching { getter() } }

    val isSuccess: Boolean
        get() = _isSuccess
    private var _isSuccess = false

    private val mutex = Mutex()
    private val injections = mutableListOf<suspend Result<T>.() -> Unit>()
    suspend fun value() = runCatching {
        mutex.withLock {
            val t = _data.value
            injections.forEach { it(t) }
            injections.clear()
            _isSuccess = t.isSuccess
            t.getOrThrow()
        }
    }

    fun inject(block: Result<T>.() -> Unit) {
        if (_data.isInitialized()) block(_data.value)
        else injections.add(block)
    }

    suspend fun injectSuspended(block: suspend Result<T>.() -> Unit) {
        if (_data.isInitialized()) _data.value.block()
        else mutex.withLock { injections.add(block) }
    }
}