package dev.brahmkshatriya.echo.ui.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

abstract class ErrorPagingSource<Key : Any, Value : Any> : PagingSource<Key, Value>() {
    fun toFlow() = Pager(
        config = config,
        pagingSourceFactory = { this }
    ).flow.flowOn(Dispatchers.IO)

    override val keyReuseSupported = true

    abstract val config: PagingConfig

    override suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value> {
        return try {
            withContext(Dispatchers.IO) { loadData(params) }
        } catch (e: Throwable) {
            LoadResult.Error(e)
        }
    }

    abstract suspend fun loadData(params: LoadParams<Key>): LoadResult.Page<Key, Value>
}