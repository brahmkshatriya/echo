package dev.brahmkshatriya.echo.utils

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

fun <T> Fragment.observe(flow: Flow<T>, callback: suspend (T) -> Unit) {
    viewLifecycleOwner.observe(flow, callback)
}

fun <T> LifecycleOwner.observe(flow: Flow<T>, block: suspend (T) -> Unit) {
    lifecycleScope.launch {
        flow.flowWithLifecycle(lifecycle).collectLatest(block)
    }
}

fun <T> LifecycleOwner.emit(flow: MutableSharedFlow<T>, block: () -> T) {
    lifecycleScope.launch { flow.emit(block()) }
}

fun LifecycleOwner.emit(flow: MutableSharedFlow<Unit>) {
    lifecycleScope.launch { flow.emit(Unit) }
}


fun <T> CoroutineScope.observe(flow: Flow<T>, block: suspend (T) -> Unit) {
    launch { flow.collectLatest(block) }
}

fun <T> Flow<T>.catchWith(throwable: MutableSharedFlow<Throwable>): Flow<T> = catch {
    throwable.emit(it)
}
