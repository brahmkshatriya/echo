package dev.brahmkshatriya.echo.extensions

import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.extensions.exceptions.AppException.Companion.toAppException
import dev.brahmkshatriya.echo.extensions.exceptions.ExtensionNotFoundException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

object ExtensionUtils {

    suspend fun <R> Extension<*>.with(
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

    suspend inline fun <reified C, R> Extension<*>.get(
        crossinline block: suspend C.() -> R
    ): Result<R> = runCatching {
        runCatching {
            val client = instance.value().getOrThrow() as? C
                ?: throw ClientException.NotSupported(C::class.simpleName ?: "")
            block(client)
        }.getOrElse { throw it.toAppException(this) }
    }

    suspend fun Extension<*>.inject(
        throwableFlow: MutableSharedFlow<Throwable>,
        block: suspend ExtensionClient.() -> Unit
    ) = runCatching {
        instance.injectOrRun { block() }
    }.getOrElse {
        throwableFlow.emit(it.toAppException(this))
    }

    suspend inline fun <reified T> Extension<*>.injectWith(
        throwableFlow: MutableSharedFlow<Throwable>,
        crossinline block: suspend T.() -> Unit
    ) = runCatching {
        instance.injectOrRun { if (this is T) block() }
    }.getOrElse {
        throwableFlow.emit(it.toAppException(this))
    }

    suspend inline fun <reified T> Extension<*>.isClient() = instance.value().getOrNull() is T
    suspend fun <T : Extension<*>> StateFlow<List<T>?>.await(): List<T> {
        return first { it != null }!!
    }
    suspend fun StateFlow<List<Extension<*>>?>.getExtension(id: String?) =
        await().find { it.id == id }

    fun StateFlow<List<Extension<*>>?>.getExtensionOrThrow(id: String?) =
        value?.find { it.id == id } ?: throw ExtensionNotFoundException(id)
}