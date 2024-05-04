package dev.brahmkshatriya.echo.utils

import android.content.Context
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture

fun <T> Context.listenFuture(future: ListenableFuture<T>, block: (Result<T>) -> Unit) {
    future.addListener({
        val result = runCatching { future.get() }
        block(result)
    }, ContextCompat.getMainExecutor(this))
}