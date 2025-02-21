package dev.brahmkshatriya.echo.utils

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

object CoroutineUtils {
    fun setDebug() {
        System.setProperty(
            kotlinx.coroutines.DEBUG_PROPERTY_NAME,
            kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
        )
    }

    suspend fun <T, R> Flow<T>.collectWith(flow: Flow<R>, block: suspend (T, R) -> Unit) {
        this.combine(flow) { t, r -> t to r }.collectLatest { (t, r) -> block(t, r) }
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
}