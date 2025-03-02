package dev.brahmkshatriya.echo.ui.common

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

object PagingUtils {
    fun <T : Any> PagedData<T>.toFlow() =
        ContinuationSource({ loadList(it) }, { invalidate(it) }).toFlow()

    suspend fun <T : Any> Flow<PagingData<T>>.collectWith(
        throwableFlow: MutableSharedFlow<Throwable>,
        collector: FlowCollector<PagingData<T>>
    ) = coroutineScope { cachedIn(this).catch { throwableFlow.emit(it) }.collect(collector) }

    class ContinuationSource<Value : Any>(
        private val load: suspend (token: String?) -> Page<Value>,
        private val invalidate: (token: String?) -> Unit
    ) : PagingSource<String, Value>() {

        fun toFlow() = Pager(
            config = config,
            pagingSourceFactory = { this }
        ).flow.flowOn(Dispatchers.IO)

        override val keyReuseSupported = true

        override suspend fun load(params: LoadParams<String>): LoadResult<String, Value> {
            return try {
                withContext(Dispatchers.IO) { loadData(params) }
            } catch (e: Throwable) {
                LoadResult.Error(e)
            }
        }

        private val config = PagingConfig(pageSize = 10, enablePlaceholders = false)
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