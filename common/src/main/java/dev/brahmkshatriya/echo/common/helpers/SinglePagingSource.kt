package dev.brahmkshatriya.echo.common.helpers

import androidx.paging.PagingConfig
import androidx.paging.PagingState

class SinglePagingSource<Value : Any>(private val data: suspend () -> List<Value>) :
    ErrorPagingSource<Int, Value>() {
    override fun getRefreshKey(state: PagingState<Int, Value>): Int? {
        return null
    }

    override val config = PagingConfig(pageSize = 1)

    override suspend fun loadData(params: LoadParams<Int>) = LoadResult.Page<Int, Value>(
        data = data(),
        prevKey = null,
        nextKey = null
    )
}