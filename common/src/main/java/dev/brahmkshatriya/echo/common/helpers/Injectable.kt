package dev.brahmkshatriya.echo.common.helpers

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Injectable<T>(
    private val getter: () -> T
) {

    val data = lazy { runCatching { getter() } }
    private val injections: MutableList<suspend T.() -> Unit> = mutableListOf()
    private val mutex = Mutex()
    val value: T?
        get() = data.value.getOrNull()

    suspend fun value() = runCatching {
        mutex.withLock {
            val t = data.value.getOrThrow()
            injections.forEach { it(t) }
            injections.clear()
            t
        }
    }

    suspend fun injectOnce(block: suspend T.() -> Unit) {
        if (!data.isInitialized() && injections.isEmpty()) mutex.withLock { injections.add(block) }
    }

    suspend fun injectOrRun(block: suspend T.() -> Unit) {
        if (data.isInitialized()) data.value.getOrThrow().block()
        else mutex.withLock { injections.add(block) }
    }

    @Suppress("UNCHECKED_CAST")
    fun <R> casted() = run {
        injections.add { this as R }
        this as Injectable<R>
    }
}