package dev.brahmkshatriya.echo.ui.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun <X> Flow<X>.observeFlow(lifecycleOwner: LifecycleOwner, callback: suspend (X) -> Unit) =
    lifecycleOwner.lifecycleScope.launch {
        flowWithLifecycle(
            lifecycleOwner.lifecycle,
            Lifecycle.State.STARTED
        ).collect(callback)
    }