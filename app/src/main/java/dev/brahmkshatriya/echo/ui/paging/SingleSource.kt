package dev.brahmkshatriya.echo.ui.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingState

class SingleSource<Value : Any>(
    private val data: suspend () -> List<Value>,
    private val clear: () -> Unit
) : ErrorPagingSource<Int, Value>() {
    override fun getRefreshKey(state: PagingState<Int, Value>): Int? {
        clear()
        return null
    }

    override val config = PagingConfig(pageSize = 1)

    override suspend fun loadData(params: LoadParams<Int>) = run {
        LoadResult.Page<Int, Value>(
            data = data(),
            prevKey = null,
            nextKey = null
        )
    }
}