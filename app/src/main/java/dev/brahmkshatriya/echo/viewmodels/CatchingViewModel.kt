package dev.brahmkshatriya.echo.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dev.brahmkshatriya.echo.plugger.echo.ExtensionInfo
import dev.brahmkshatriya.echo.ui.exception.AppException.Companion.toAppException
import dev.brahmkshatriya.echo.utils.catchWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow

abstract class CatchingViewModel(
    val throwableFlow: MutableSharedFlow<Throwable>
) : ViewModel() {

    private var initialized = false

    open fun onInitialize() {}

    fun initialize() {
        if (initialized) return
        initialized = true
        onInitialize()
    }

    suspend fun <T> tryWith(extensionInfo: ExtensionInfo, block: suspend () -> T) =
        tryWith(throwableFlow, extensionInfo, block)

    suspend fun <T : Any> Flow<PagingData<T>>.collectTo(
        collector: FlowCollector<PagingData<T>>
    ) = cachedIn(viewModelScope).catchWith(throwableFlow).collect(collector)

    companion object {
        @Suppress("USELESS_CAST")
        suspend fun <T> tryWith(
            throwableFlow: MutableSharedFlow<Throwable>,
            info: ExtensionInfo,
            block: suspend () -> T
        ) = runCatching {
            block() as T
        }.getOrElse {
            throwableFlow.emit(it.toAppException(info))
            it.printStackTrace()
            null
        }
    }
}