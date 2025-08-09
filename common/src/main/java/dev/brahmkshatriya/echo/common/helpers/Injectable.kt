package dev.brahmkshatriya.echo.common.helpers

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Injectable<T>(
    private val getter: () -> T,
    private var injections: List<suspend T.() -> Unit>
) {

    val data = lazy { runCatching { getter() } }
    private val mutex = Mutex()
    val value: T?
        get() = data.value.getOrNull()


    suspend fun value() = runCatching {
        mutex.withLock {
            val t = data.value.getOrThrow()
            injections.forEach { it(t) }
            injections = emptyList()
            injectionsMap.values.forEach { it(t) }
            injectionsMap.clear()
            t
        }
    }

    private val injectionsMap = mutableMapOf<String, suspend T.() -> Unit>()
    suspend fun injectOrRun(id: String, block: suspend T.() -> Unit) {
        if (data.isInitialized()) data.value.getOrThrow().block()
        else mutex.withLock {
            injectionsMap[id] = block
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <R> casted() = run {
        if (data.isInitialized()) throw IllegalStateException("Cannot cast an already initialized Injectable")
        injections = injections + listOf { this as R }
        this as Injectable<R>
    }
}