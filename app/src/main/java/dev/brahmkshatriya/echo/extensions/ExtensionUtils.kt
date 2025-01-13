package dev.brahmkshatriya.echo.extensions

import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.ui.exception.AppException.Companion.toAppException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow

suspend fun <T : ExtensionClient, R> Extension<T>.with(
    throwableFlow: MutableSharedFlow<Throwable>,
    block: suspend () -> R
): R? = runCatching {
    block()
}.getOrElse {
    throwableFlow.emit(it.toAppException(this))
    it.printStackTrace()
    null
}

suspend fun <T : ExtensionClient, R> Extension<T>.run(
    throwableFlow: MutableSharedFlow<Throwable>,
    block: suspend T.() -> R
): R? = with(throwableFlow) {
    block(instance.value().getOrThrow())
}

suspend inline fun <reified C, R> Extension<*>.get(
    throwableFlow: MutableSharedFlow<Throwable>,
    crossinline block: suspend C.() -> R
): R? = with(throwableFlow) {
    val client = instance.value().getOrThrow() as? C ?: return@with null
    block(client)
}

suspend inline fun <reified T> Extension<*>.inject(crossinline block: suspend T.() -> Unit) {
    instance.injectSuspended { (getOrNull() as? T)?.block() }
}

suspend inline fun <reified T> Extension<*>.isClient() = instance.value().getOrNull() is T

fun StateFlow<List<Extension<*>>?>.getExtension(id: String?) =
    value?.find { it.metadata.id == id }