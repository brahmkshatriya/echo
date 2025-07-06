package dev.brahmkshatriya.echo.extensions

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extensions.exceptions.AppException.Companion.toAppException
import dev.brahmkshatriya.echo.extensions.exceptions.ExtensionNotFoundException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow

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

    suspend inline fun <reified C, R> Extension<*>.run(
        crossinline block: suspend C.() -> R
    ): Result<R?> = runCatching top@{
        runCatching {
            val client = instance.value().getOrThrow() as? C ?: return@runCatching null
            block(client)
        }.getOrElse { throw it.toAppException(this) }
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

    fun <T : Extension<*>> StateFlow<List<T>>.getExtension(id: String?) =
        value.find { it.id == id }

    fun <T : Extension<*>> StateFlow<List<T>>.getExtensionOrThrow(id: String?) =
        value.find { it.id == id } ?: throw ExtensionNotFoundException(id)

    fun extensionPrefId(extensionType: String, extensionId: String) = "$extensionType-$extensionId"
    fun String.prefs(context: Context) =
        context.getSharedPreferences(this, Context.MODE_PRIVATE)!!

    fun Metadata.prefs(context: Context) = extensionPrefId(type.name, id).prefs(context)
    fun Extension<*>.prefs(context: Context) = metadata.prefs(context)

    fun getSettings(context: Context, metadata: Metadata): Settings {
        return toSettings(metadata.prefs(context))
    }

    fun toSettings(prefs: SharedPreferences) = object : Settings {
        override fun getString(key: String) = prefs.getString(key, null)
        override fun putString(key: String, value: String?) {
            prefs.edit { putString(key, value) }
        }

        override fun getInt(key: String) =
            if (prefs.contains(key)) prefs.getInt(key, 0) else null

        override fun putInt(key: String, value: Int?) {
            prefs.edit { if (value != null) putInt(key, value) else remove(key) }
        }

        override fun getBoolean(key: String) =
            if (prefs.contains(key)) prefs.getBoolean(key, false) else null

        override fun putBoolean(key: String, value: Boolean?) {
            prefs.edit { if (value != null) putBoolean(key, value) else remove(key) }
        }

        override fun getStringSet(key: String) = prefs.getStringSet(key, null)
        override fun putStringSet(key: String, value: Set<String>?) {
            prefs.edit { putStringSet(key, value) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun SharedPreferences.copyTo(dest: SharedPreferences) = with(dest.edit()) {
        all.entries.forEach { entry ->
            val value = entry.value ?: return@forEach
            val key = entry.key
            when (value) {
                is String -> putString(key, value)
                is Set<*> -> putStringSet(key, value as Set<String>)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is Boolean -> putBoolean(key, value)
                else -> {}
            }
        }
        apply()
    }
}