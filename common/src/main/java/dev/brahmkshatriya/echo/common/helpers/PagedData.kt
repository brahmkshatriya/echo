package dev.brahmkshatriya.echo.common.helpers

import androidx.paging.Pager
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

sealed class PagedData<Value : Any>(
    val flow: Flow<PagingData<Value>>
) : Flow<PagingData<Value>> {
    override suspend fun collect(collector: FlowCollector<PagingData<Value>>) {
        flow.collect(collector)
    }

    class Source<Key : Any, Value : Any>(
        source: ErrorPagingSource<Key, Value>,
        pager: Pager<Key, Value> = Pager(
            config = source.config,
            pagingSourceFactory = { source }
        )
    ) : PagedData<Value>(pager.flow)

    class Single<Value : Any>(
        data: suspend () -> List<Value>
    ) : PagedData<Value>(
        SinglePagingSource(data).let {
            Pager(
                config = it.config,
                pagingSourceFactory = { it }
            ).flow
        }
    )

}