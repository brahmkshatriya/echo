package dev.brahmkshatriya.echo.common.helpers

import androidx.paging.PagingConfig
import androidx.paging.PagingSource

abstract class ErrorPagingSource<Key : Any, Value : Any> : PagingSource<Key, Value>() {
    abstract val config: PagingConfig

    override suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value> {
        return try {
            loadData(params)
        } catch (e: Throwable) {
            LoadResult.Error(e)
        }
    }

    abstract suspend fun loadData(params: LoadParams<Key>): LoadResult.Page<Key, Value>
}