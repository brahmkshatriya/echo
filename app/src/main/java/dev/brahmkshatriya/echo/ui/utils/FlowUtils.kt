package dev.brahmkshatriya.echo.ui.utils

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

fun <T> Fragment.observe(flow: Flow<T>, callback: suspend (T) -> Unit) {
    viewLifecycleOwner.observe(flow, callback)
}

fun <T> LifecycleOwner.observe(flow: Flow<T>, block: suspend (T) -> Unit) {
    lifecycleScope.launch {
        flow.flowWithLifecycle(lifecycle).collect(block)
    }
}

fun <T> LifecycleOwner.emit(flow: MutableSharedFlow<T>, block: () -> T) {
    lifecycleScope.launch { flow.emit(block()) }
}