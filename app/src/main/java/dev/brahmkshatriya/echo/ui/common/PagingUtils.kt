package dev.brahmkshatriya.echo.ui.common

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.extensions.exceptions.AppException.Companion.toAppException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

object PagingUtils {

    data class Data<T : Any>(
        val extension: Extension<*>?,
        val id: String?,
        val pagedData: PagedData<T>?,
        val pagingData: PagingData<T>?
    )

    suspend fun <T : Any> PagedData<T>.load(extension: Extension<*>, token: String?) = runCatching {
        runCatching {
            loadList(token)
        }.getOrElse {
            throw it.toAppException(extension)
        }
    }

    fun <T : Any> errorPagingData(throwable: Throwable) = PagingData.empty<T>(
        LoadStates(
            LoadState.Error(throwable),
            LoadState.NotLoading(false),
            LoadState.NotLoading(true)
        )
    )

    fun <T : Any> loadingPagingData() = PagingData.empty<T>(
        LoadStates(
            LoadState.Loading,
            LoadState.NotLoading(false),
            LoadState.NotLoading(true)
        )
    )

    fun <T : Any> from(shelves: List<T>): PagingData<T> {
        return PagingData.from(
            shelves, LoadStates(
                LoadState.NotLoading(false),
                LoadState.NotLoading(false),
                LoadState.NotLoading(true)
            )
        )
    }


    fun <T : Any> PagedData<T>.toFlow(extension: Extension<*>) =
        ContinuationSource(extension, { loadList(it) }, { invalidate(it) }).toFlow()

    suspend fun <T : Any> Flow<PagingData<T>>.collectWith(
        throwableFlow: MutableSharedFlow<Throwable>,
        collector: FlowCollector<PagingData<T>>
    ) = coroutineScope { cachedIn(this).catch { throwableFlow.emit(it) }.collect(collector) }


    private class ContinuationSource<Value : Any>(
        private val extension: Extension<*>,
        private val load: suspend (token: String?) -> Page<Value>,
        private val invalidate: (token: String?) -> Unit
    ) : PagingSource<String, Value>() {

        fun toFlow() = Pager(
            config = config,
            pagingSourceFactory = { this }
        ).flow.flowOn(Dispatchers.IO)

        override val keyReuseSupported = true

        override suspend fun load(params: LoadParams<String>): LoadResult<String, Value> {
            return runCatching {
                withContext(Dispatchers.IO) { loadData(params) }
            }.getOrElse {
                LoadResult.Error(it.toAppException(extension))
            }
        }

        private val config = PagingConfig(
            pageSize = 50,
            enablePlaceholders = false,
            prefetchDistance = 100
        )

        private suspend fun loadData(params: LoadParams<String>): LoadResult.Page<String, Value> {
            val token = params.key
            val page = load(token)
            return LoadResult.Page(
                data = page.data,
                prevKey = null,
                nextKey = page.continuation
            )
        }

        override fun getRefreshKey(state: PagingState<String, Value>) =
            state.anchorPosition?.let { pos ->
                state.closestPageToPosition(pos)?.nextKey?.let { key ->
                    invalidate(key)
                    key
                }
            }
    }
}