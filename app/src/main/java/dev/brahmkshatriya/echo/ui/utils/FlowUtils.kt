package dev.brahmkshatriya.echo.ui.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

fun <X> Flow<X>.observeFlow(lifecycleOwner: LifecycleOwner, callback: suspend (X) -> Unit) =
    lifecycleOwner.lifecycleScope.launch {
        flowWithLifecycle(
            lifecycleOwner.lifecycle,
            Lifecycle.State.STARTED
        ).collect(callback)
    }

fun <T> CoroutineScope.collect(flow: MutableStateFlow<T>, block: suspend (T) -> Unit) {
    launch { flow.collectLatest(block) }
}

fun <T> CoroutineScope.collectAndRemove(flow: MutableStateFlow<T?>, block: suspend (T) -> Unit) {
    launch {
        flow.collectLatest {
            it ?: return@collectLatest
            block(it)
            flow.value = null
        }
    }
}