package dev.brahmkshatriya.echo.utils

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

fun <T> Fragment.observe(flow: Flow<T>, callback: suspend (T) -> Unit) {
    viewLifecycleOwner.observe(flow, callback)
}

fun <T> LifecycleOwner.observe(flow: Flow<T>, block: suspend (T) -> Unit) {
    lifecycleScope.launch {
        flow.flowWithLifecycle(lifecycle).collectLatest(block)
    }
}

fun <T> LifecycleOwner.collect(flow: Flow<T>, block: suspend (T) -> Unit) {
    lifecycleScope.launch { flow.collect(block) }
}

fun <T> LifecycleOwner.emit(flow: MutableSharedFlow<T>, block: () -> T) {
    val it = block()
    if (!flow.tryEmit(it)) lifecycleScope.launch {
        flow.emit(it)
    }
}

fun LifecycleOwner.emit(flow: MutableSharedFlow<Unit>) {
    emit(flow) {}
}


fun <T> CoroutineScope.collect(flow: Flow<T>, block: suspend (T) -> Unit) {
    launch { flow.collectLatest(block) }
}

fun <T> Flow<T>.catchWith(throwable: MutableSharedFlow<Throwable>): Flow<T> = catch {
    throwable.emit(it)
}


class DerivedStateFlow<T>(
    private val getValue: () -> T,
    private val flow: Flow<T>
) : StateFlow<T> {

    override val replayCache: List<T>
        get () = listOf(value)

    override val value: T
        get () = getValue()

    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        coroutineScope { flow.distinctUntilChanged().stateIn(this).collect(collector) }
    }
}

fun <T, R> StateFlow<T>.mapState(transform: (a: T) -> R): StateFlow<R> {
    return DerivedStateFlow(
        getValue = { transform(this.value) },
        flow = this.map { a -> transform(a) }
    )
}