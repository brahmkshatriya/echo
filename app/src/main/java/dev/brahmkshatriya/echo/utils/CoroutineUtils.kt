package dev.brahmkshatriya.echo.utils

import android.content.Context
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.ExperimentalTypeInference

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

    fun <T> Flow<T>.throttleLatest(delayMillis: Long): Flow<T> = conflate().transform {
        emit(it)
        delay(delayMillis)
    }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalTypeInference::class)
    inline fun <reified T, R> combineTransformLatest(
        vararg flows: Flow<T>,
        @BuilderInference noinline transform: suspend FlowCollector<R>.(Array<T>) -> Unit
    ): Flow<R> {
        return combine(*flows) { it }
            .transformLatest(transform)
    }

    @OptIn(ExperimentalTypeInference::class)
    fun <T1, T2, R> Flow<T1>.combineTransformLatest(
        flow2: Flow<T2>,
        @BuilderInference transform: suspend FlowCollector<R>.(T1, T2) -> Unit
    ): Flow<R> {
        return combineTransformLatest(this, flow2) { args ->
            @Suppress("UNCHECKED_CAST")
            transform(
                args[0] as T1,
                args[1] as T2
            )
        }
    }

    fun <T> CoroutineScope.future(
        context: CoroutineContext = Dispatchers.IO, block: suspend () -> T
    ): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        launch(context) {
            future.set(block())
        }
        return future
    }

    fun <T> CoroutineScope.futureCatching(
        context: CoroutineContext = Dispatchers.IO, block: suspend () -> T
    ): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        launch(context) {
            runCatching {
                future.set(block())
            }.getOrElse {
                future.setException(it)
            }
        }
        return future
    }


    suspend fun <T> ListenableFuture<T>.await(context: Context) = suspendCancellableCoroutine {
        it.invokeOnCancellation {
            cancel(true)
        }
        addListener({
            it.resumeWith(runCatching { get()!! })
        }, ContextCompat.getMainExecutor(context))
    }
}