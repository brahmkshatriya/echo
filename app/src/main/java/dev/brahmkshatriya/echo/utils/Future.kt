package dev.brahmkshatriya.echo.utils

import android.content.Context
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun <T> Context.listenFuture(future: ListenableFuture<T>, block: (Result<T>) -> Unit) {
    future.addListener({
        val result = runCatching { future.get() }
        block(result)
    }, ContextCompat.getMainExecutor(this))
}

fun <T> CoroutineScope.future(block: suspend () -> T): ListenableFuture<T> {
    val future = SettableFuture.create<T>()
    launch {
        runCatching {
            future.set(block())
        }.onFailure {
            future.setException(it)
        }
    }
    return future
}